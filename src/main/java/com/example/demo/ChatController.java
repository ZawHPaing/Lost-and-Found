package com.example.demo;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/chat")
public class ChatController {

	@Autowired
	private ChatRepository chatRepository;

	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ChatMediaRepository chatMediaRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@PostMapping("/contact-admin")
	public String contactAdmin(HttpSession session) {
	    User loggedInUser = (User) session.getAttribute("loggedInUser");

	    // Check if user is logged in
	    if (loggedInUser == null) {
	        return "redirect:/users/login"; // Redirect if not logged in
	    }

	    String userRole = loggedInUser.getRole();

	    // Ensure that only users with role "ROLE_USER" can contact admin
	    if ("ROLE_ADMIN".equals(userRole)) {
	        // If the logged-in user is an admin (role "ROLE_ADMIN"), prevent contacting admin
	        return "redirect:/error?message=Admins cannot contact themselves.";
	    }

	    // Find the admin user (role "ROLE_ADMIN")
	    List<User> adminUsers = userRepository.findByRole("ROLE_ADMIN"); // Find admin user by role
	    if (adminUsers.isEmpty()) {
	        return "redirect:/error?message=Admin user not found";
	    }

	    User adminUser = adminUsers.get(0); // Assuming there's only one admin

	    // Check if a chat room already exists between the logged-in user and the admin
	    Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findByUsers(loggedInUser.getUserId(), adminUser.getUserId());
	    if (chatRoomOpt.isPresent()) {
	        // If chat room exists, redirect to it
	        return "redirect:/chat/room/" + chatRoomOpt.get().getChatRoomId();
	    } else {
	        // If no chat room exists, create a new one
	        ChatRoom chatRoom = new ChatRoom();
	        chatRoom.setSender(loggedInUser);
	        chatRoom.setRecipient(adminUser);
	        chatRoomRepository.save(chatRoom); // Save the new chat room

	        // Redirect to the newly created chat room
	        return "redirect:/chat/room/" + chatRoom.getChatRoomId();
	    }
	}


	/**
	 * Create or retrieve a chat room between two users and render the chat view.
	 */
	@PostMapping("/room")
	@Transactional
	public String createOrUpdateChatRoom(@RequestParam Integer userId2, HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser == null) {
			return "redirect:/users/login"; // Redirect if not logged in
		}

		if (userId2 == null || Objects.equals(loggedInUser.getUserId(), userId2)) {
			return "redirect:/error?message=Cannot chat with yourself";
		}

		Optional<User> recipientOpt = userRepository.findById(userId2);
		if (recipientOpt.isEmpty()) {
			return "redirect:/error?message=Recipient user not found";
		}

		User recipient = recipientOpt.get();
		Integer senderId = loggedInUser.getUserId();
		Integer recipientId = recipient.getUserId();

		synchronized (this) {
			Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findByUsers(senderId, recipientId);

			if (chatRoomOpt.isPresent()) {
				return "redirect:/chat/room/" + chatRoomOpt.get().getChatRoomId();
			}

			ChatRoom chatRoom = new ChatRoom();
			if (senderId < recipientId) {
				chatRoom.setSender(loggedInUser);
				chatRoom.setRecipient(recipient);
			} else {
				chatRoom.setSender(recipient);
				chatRoom.setRecipient(loggedInUser);
			}

			chatRoomRepository.save(chatRoom);
			return "redirect:/chat/room/" + chatRoom.getChatRoomId();
		}
	}

	@GetMapping("/room/{chatRoomId}")
	  public String getChatRoom(@PathVariable int chatRoomId, Model model, HttpSession session) {
	    Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatRoomId);

	    if (chatRoomOpt.isEmpty()) {
	      model.addAttribute("error", "Chat room not found");
	      return "error";
	    }

	    ChatRoom chatRoom = chatRoomOpt.get();
	    User loggedInUser = (User) session.getAttribute("loggedInUser");
	    //System.out.println("omom: " + chatRoom.getSender().getUserId());
	    User sender = null;
	    
	    //User sender = chatRoom.getRecipient().equals(loggedInUser) ? chatRoom.getSender() : chatRoom.getRecipient();
	    
	    if (chatRoom.getSender().getUserId() == loggedInUser.getUserId()) {
	       sender = chatRoom.getRecipient();
	    } else if (chatRoom.getRecipient().getUserId() == loggedInUser.getUserId()) {
	       sender = chatRoom.getSender();
	    } 
	    // Pass recipient's data to the front-end
	    model.addAttribute("currentUserId", loggedInUser.getUserId());
	    model.addAttribute("otherUserId",sender.getUserId());
	    model.addAttribute("otherUserProfilePic",sender.getProfilePicture());
	    model.addAttribute("otherUserName", sender.getUsername()); // ✅ Add recipient's name

	    return "chat"; // Returns chat.html
	  }

	@GetMapping("/room/{chatRoomId}/messages")
	@ResponseBody
	public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable int chatRoomId) {
		Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);
		if (chatRoom.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		List<ChatMessage> messages = chatRepository.findByChatRoom_ChatRoomId(chatRoomId);
		return ResponseEntity.ok(messages);
	}

	/**
	 * REST API to send a message and save it in the database.
	 */
	// Combined method for sending both messages and media
	@PostMapping("/send")
	public ResponseEntity<?> sendMessageAndMedia(@RequestParam int chatRoomId,
			@RequestParam(required = false) String content,
			@RequestParam(value = "files", required = false) MultipartFile[] files, HttpSession session) {

		User loggedInUser = (User) session.getAttribute("loggedInUser");
		Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatRoomId);
		if (chatRoomOpt.isEmpty()) {
			return ResponseEntity.status(404).body(Map.of("error", "Chat room not found."));
		}
		User recipient;
		ChatRoom chatRoom = chatRoomOpt.get();
		if (chatRoom.getSender().getUserId() == loggedInUser.getUserId()) {
			recipient = chatRoom.getRecipient();
		} else {
			recipient = chatRoom.getSender();
		}

		// Create a message object to save text or media
		ChatMessage message = new ChatMessage();
		message.setChatRoom(chatRoom);
		message.setSender(loggedInUser);
		message.setRecipient(recipient);
		message.setRead(false);

		if (content != null && !content.isEmpty()) {
			message.setContent(content);
		}

		// Save the text message if there is any
		chatRepository.save(message);

		// Create and save a notification for the recipient
		Notification notification = new Notification(recipient, // recipient user
				loggedInUser,
				message.getChatRoom().getChatRoomId(), // ✅ Use chatRoomId instead of chatId
				"chat_received", // notification type
				"You have a new message from " + loggedInUser.getUsername() // notification message
		);
		notificationRepository.save(notification);

		// Handle media files
		if (files != null && files.length > 0) {
			List<ChatMedia> mediaList = new ArrayList<>();
			try {
				for (MultipartFile file : files) {
					if (file.isEmpty()) {
						continue;
					}

					String contentType = file.getContentType();
					if (contentType == null || (!contentType.startsWith("image") && !contentType.startsWith("video"))) {
						return ResponseEntity.status(400).body(Map.of("error", "Invalid file type."));
					}

					// Save files to the specified folder
					String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
					Path uploadDir = Paths.get("D:/LostAndFoundWithoutSecurity/uploads/chats");  // Updated path
					// Create the directory if it doesn't exist
					if (!Files.exists(uploadDir)) {
					    Files.createDirectories(uploadDir);
					}
					Path filePath = uploadDir.resolve(fileName);
					Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

					String fileUrl = "/uploads/chats/" + fileName;
					String mediaType = contentType.startsWith("image") ? "image" : "video";

					// Save media record in the database
					ChatMedia media = new ChatMedia(message, fileUrl, mediaType);
					chatMediaRepository.save(media);

					mediaList.add(media); // Add media to the list
				}

				// Set the media list to the message
				message.setMediaList(mediaList);

				return ResponseEntity.ok(Map.of("success", true, "message", message, "files", mediaList));
			} catch (IOException e) {
				e.printStackTrace();
				return ResponseEntity.status(500).body(Map.of("error", "Error uploading files."));
			}
		}

		// If there were no files but there was a message, return success with the text
		// message
		return ResponseEntity.ok(Map.of("success", true, "message", message));
	}

	/**
	 * WebSocket: Broadcast message to all chat room participants.
	 */
	@MessageMapping("/send-message/{chatRoomId}")
	@SendTo("/topic/chatroom-{chatRoomId}")
	public ChatMessage broadcastMessage(@DestinationVariable int chatRoomId, ChatMessage message) {
		// Extract sender ID from message
		Integer senderId = message.getSender().getUserId();

		// Fetch sender and chat room from the database
		Optional<User> senderOpt = userRepository.findById(senderId);
		Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findById(chatRoomId);

		if (senderOpt.isEmpty() || chatRoomOpt.isEmpty()) {
			throw new IllegalArgumentException("Sender or chat room not found.");
		}

		User sender = senderOpt.get();
		ChatRoom chatRoom = chatRoomOpt.get();

		// Correctly assign the recipient
		User recipient = chatRoom.getSender().equals(sender) ? chatRoom.getRecipient() : chatRoom.getSender();

		// Update message details before saving
		message.setSender(sender);
		message.setRecipient(recipient);
		message.setChatRoom(chatRoom);

//        chatRepository.save(message);
		return message; // This will be sent to WebSocket subscribers
	}

	@GetMapping("/notifications")
	public ResponseEntity<List<Notification>> getNotifications(HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return ResponseEntity.status(401).body(new ArrayList<>()); // Unauthorized if no logged-in user
		}

		List<Notification> notifications = notificationRepository.findByUser(loggedInUser);
		return ResponseEntity.ok(notifications);
	}

	@GetMapping("/notifications/view")
	public String viewNotifications(Model model, HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return "redirect:/users/login"; // Redirect to login if not logged in
		}

		List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(loggedInUser);
		model.addAttribute("notifications", notifications);

		return "notifications"; // Render `notifications.html`
	}

	@GetMapping("/notifications/unread-count")
	public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
//        if (loggedInUser == null) {
//            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
//        }

		Long unreadCount = notificationRepository.countByUserAndSeenFalse(loggedInUser);
		return ResponseEntity.ok(Map.of("count", unreadCount));
	}

	@PostMapping("/notifications/mark-all-seen")
	public ResponseEntity<?> markAllNotificationsAsSeen(HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		if (loggedInUser == null) {
			return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
		}

		List<Notification> unreadNotifications = notificationRepository.findByUserAndSeenFalse(loggedInUser);
		unreadNotifications.forEach(notification -> notification.setSeen(true));
		notificationRepository.saveAll(unreadNotifications);

		return ResponseEntity.ok(Map.of("success", true));
	}

	@PutMapping("/mark-read/{chatRoomId}")
    public ResponseEntity<?> markMessagesAndNotificationsAsRead(@PathVariable int chatRoomId, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }

        // Mark messages as read
        chatRepository.markMessagesAsRead(chatRoomId, loggedInUser.getUserId());

        // Mark chat-related notifications as seen
        notificationRepository.markNotificationsAsSeen(chatRoomId, loggedInUser.getUserId());

        return ResponseEntity.ok().body("Messages and notifications marked as read");
    }


}
