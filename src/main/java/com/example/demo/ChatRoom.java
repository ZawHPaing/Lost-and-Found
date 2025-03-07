package com.example.demo;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "ChatRooms")
@JsonIgnoreProperties({ "sender", "recipient", "hibernateLazyInitializer", "handler" })
public class ChatRoom {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int chatRoomId;

	@ManyToOne(fetch = FetchType.LAZY) // Many chat rooms can have the same sender
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;

	@ManyToOne(fetch = FetchType.LAZY) // Many chat rooms can have the same recipient
	@JoinColumn(name = "recipient_id", nullable = false)
	private User recipient;

	@OneToMany(mappedBy = "chatRoom", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JsonManagedReference // Prevents infinite recursion
	private List<ChatMessage> chatMessages;

	@Column(updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	private LocalDateTime updatedAt = LocalDateTime.now();

	// New field to store the last message
	@Transient
	private ChatMessage lastMessage;

	// Getters and setters
	public int getChatRoomId() {
		return chatRoomId;
	}

	public void setChatRoomId(int chatRoomId) {
		this.chatRoomId = chatRoomId;
	}

	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}

	public User getRecipient() {
		return recipient;
	}

	public void setRecipient(User recipient) {
		this.recipient = recipient;
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

	public List<ChatMessage> getChatMessages() {
		return chatMessages;
	}

	public void setChatMessages(List<ChatMessage> chatMessages) {
		this.chatMessages = chatMessages;
	}

	public ChatMessage getLastMessage() {
		return lastMessage;
	}

	public void setLastMessage(ChatMessage lastMessage) {
		this.lastMessage = lastMessage;
	}
}
