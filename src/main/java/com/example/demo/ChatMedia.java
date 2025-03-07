package com.example.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "chat_media")
public class ChatMedia {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int mediaId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_id", nullable = false)
	private ChatMessage chatMessage;

	@Column(name = "media_url", nullable = false)
	private String mediaUrl;

	@Column(name = "media_type", nullable = false)
	private String mediaType; // "image" or "video"

	public ChatMedia() {
	}

	public ChatMedia(ChatMessage chatMessage, String mediaUrl, String mediaType) {
		this.chatMessage = chatMessage;
		this.mediaUrl = mediaUrl;
		this.mediaType = mediaType;
	}

	// Getters and Setters
	public int getMediaId() {
		return mediaId;
	}

	public void setMediaId(int mediaId) {
		this.mediaId = mediaId;
	}

	public ChatMessage getChatMessage() {
		return chatMessage;
	}

	public void setChatMessage(ChatMessage chatMessage) {
		this.chatMessage = chatMessage;
	}

	public String getMediaUrl() {
		return mediaUrl;
	}

	public void setMediaUrl(String mediaUrl) {
		this.mediaUrl = mediaUrl;
	}

	public String getMediaType() {
		return mediaType;
	}

	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
}
