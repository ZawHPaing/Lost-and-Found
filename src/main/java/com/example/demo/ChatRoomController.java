package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/chatroom")
public class ChatRoomController {
	@Autowired
	private ChatRoomRepository chatRoomRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ChatRepository chatMessageRepository;

	@PostMapping("/create")
	public ChatRoom createChatRoom(@RequestParam int senderId, @RequestParam int recipientId) {
		Optional<User> sender = userRepository.findById(senderId);
		Optional<User> recipient = userRepository.findById(recipientId);

		if (sender.isPresent() && recipient.isPresent()) {
			Optional<ChatRoom> existingChatRoom = chatRoomRepository.findByUsers(senderId, recipientId);
			if (existingChatRoom.isPresent()) {
				throw new RuntimeException("Chat room already exists between these users.");
			}

			ChatRoom chatRoom = new ChatRoom();
			chatRoom.setSender(sender.get());
			chatRoom.setRecipient(recipient.get());
			return chatRoomRepository.save(chatRoom);
		} else {
			throw new RuntimeException("Invalid sender or recipient ID");
		}
	}

	@GetMapping("/{chatRoomId}")
	public Optional<ChatRoom> getChatRoom(@PathVariable int chatRoomId) {
		return chatRoomRepository.findById(chatRoomId);
	}

	@GetMapping("/conversations")
	public String getAllConversations(HttpSession session, Model model) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser == null) {
			throw new IllegalArgumentException("User must be logged in");
		}

		int userId = loggedInUser.getUserId(); // ✅ Get userId from session
		String userRole = loggedInUser.getRole(); 
		System.out.println("Fetching conversations for User ID: " + userId);

		List<ChatRoom> chatRooms = chatRoomRepository.findAllByUserId(userId);

		Map<Integer, Long> unreadCounts = new HashMap<>();

		chatRooms.forEach(chatRoom -> {
			ChatMessage lastMessage = chatRoom.getChatMessages().stream()
					.max(Comparator.comparing(ChatMessage::getCreatedAt)).orElse(null);
			chatRoom.setLastMessage(lastMessage);

			Long unreadCount = chatMessageRepository.countUnreadMessagesForChatRoom(chatRoom.getChatRoomId(), userId);
			System.out.println("Unread messages found: " + unreadCount);

			unreadCounts.put(chatRoom.getChatRoomId(), unreadCount != null ? unreadCount : 0);
		});

		model.addAttribute("chatRooms", chatRooms);
		model.addAttribute("unreadCounts", unreadCounts);
		model.addAttribute("loggedInUserId", userId);
		model.addAttribute("userRole", userRole); 
		return "chatHistory";
	}

}
