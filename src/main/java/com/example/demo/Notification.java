package com.example.demo;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int notificationId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	@JsonIgnore
	private User user; // The user to whom the notification is directed
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sender_id")
	@JsonIgnore
	private User sender; // The user to whom the notification is directed

	@Column(nullable = false)
	private int referenceId; // ID of related entity, like message_id for chat or post_id for post approval

	@Column(nullable = false, length = 50)
	private String notificationType; // Type of notification, e.g., 'chat_sent', 'post_approved'

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message; // The actual message for the notification

	@Column(nullable = false)
	private boolean seen = false;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt; // Timestamp when the notification was created

	@UpdateTimestamp
	@Column(name = "updated_at")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt; // Timestamp when the notification was last updated

	public Notification() {
	}

	public Notification(User user, User sender, int referenceId, String notificationType, String message) {
		this.user = user;
		this.sender = sender;
		this.referenceId = referenceId;
		this.notificationType = notificationType;
		this.message = message;
	}

	public int getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(int notificationId) {
		this.notificationId = notificationId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
	
	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}


	public int getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(int referenceId) {
		this.referenceId = referenceId;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isSeen() {
		return seen;
	}

	public void setSeen(boolean seen) {
		this.seen = seen;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
	public String toString() {
		return "Notification{" + "notificationId=" + notificationId + ", user="
				+ (user != null ? user.getUserId() : "null")
				+ (sender != null ? sender.getUserId() : null) 
				+ ", referenceId=" + referenceId + ", notificationType='"
				+ notificationType + '\'' + ", message='" + message + '\'' + ", isSeen=" + seen + ", createdAt="
				+ createdAt + ", updatedAt=" + updatedAt + '}';
	}
}
