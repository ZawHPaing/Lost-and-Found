package com.example.demo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class NotificationController {

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ChatRepository messageRepository;

	@GetMapping("/notifications/findById/{notiId}")
	public ResponseEntity<?> getNotificationById(@PathVariable("notiId") int notiId) {
		Optional<Notification> notification = notificationRepository.findByNotificationId(notiId);
		if (notification.isPresent()) {
			Notification foundNotification = notification.get();
			Integer senderId = foundNotification.getSender() != null ? foundNotification.getSender().getUserId() : null;
	        // Return just the senderId in the response
	        return ResponseEntity.ok(senderId);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found.");
		}
	}

	@GetMapping("/notifications")
	public String viewNotifications(Model model, HttpSession session) {
		// Retrieve logged-in user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser == null) {
			return "redirect:/login"; // Redirect if no user is logged in
		} else {
			// Fetch notifications for the logged-in user
			List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(loggedInUser);

			Long unreadNoti = 0L;
			Long unreadMsg = 0L; // Default to 0 if no user is logged in

			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);

			model.addAttribute("totalUnreadNoti", unreadNoti);
			model.addAttribute("totalUnreadMsg", unreadMsg);
			model.addAttribute("notifications", notifications);

			if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
				return "notificationsAdmin";
			} else {
				return "notifications";
			}

		} // Return the view named "notifications"
	}

	@PutMapping("/notifications/{notificationId}/seen")
	public ResponseEntity<?> markNotificationAsSeen(@PathVariable int notificationId) {
		Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
		if (notificationOpt.isEmpty()) {
			return ResponseEntity.status(404).body(Map.of("error", "Notification not found."));
		}

		Notification notification = notificationOpt.get();
		notification.setSeen(true);
		notificationRepository.save(notification);

		return ResponseEntity.ok(Map.of("success", true));
	}

	@PutMapping("/mark-read/{chatRoomId}")
	@Transactional
	public ResponseEntity<?> markMessagesAsRead(@PathVariable int chatRoomId, HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
		}

		// Update messages to isRead = true
		messageRepository.markMessagesAsRead(chatRoomId, loggedInUser.getUserId());

		// Update related notifications to seen = true
		notificationRepository.markNotificationsAsSeen(chatRoomId, loggedInUser.getUserId());

		return ResponseEntity.ok(Map.of("success", true));
	}

	@PutMapping("/notifications/mark-all-seen")
	@Transactional
	public ResponseEntity<?> markAllNotificationsAsSeen(HttpSession session) {
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
		}

		// Mark all notifications as seen for the logged-in user
		List<Notification> notifications = notificationRepository.findByUserAndSeenFalse(loggedInUser);
		for (Notification notification : notifications) {
			notification.setSeen(true);
			notificationRepository.save(notification);

			// If the notification type is chat_received, mark related messages as read
			if ("chat_received".equals(notification.getNotificationType())) {
				messageRepository.markMessagesAsRead(notification.getReferenceId(), loggedInUser.getUserId());
			}
		}

		return ResponseEntity.ok(Map.of("success", true));
	}

}
