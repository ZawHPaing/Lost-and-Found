package com.example.demo;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "chatId")
@Entity
@Table(name = "Chats")
public class ChatMessage {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private int chatId;

	 @ManyToOne(fetch = FetchType.LAZY)
	 @JoinColumn(name = "chat_room_id", nullable = false)
	 @JsonBackReference // Prevents infinite recursion
	    private ChatRoom chatRoom;

	    @Column(nullable = true, length = 255)
	    @Size(max = 255, message = "Message must not exceed 255 characters")
	    private String content;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "recipient_id", nullable = false)
	    @JsonIgnore
	    private User recipient;

	    @ManyToOne(fetch = FetchType.LAZY)
	    @JoinColumn(name = "sender_id", nullable = false)
	    private User sender;

	    @Column(nullable = false, updatable = false)
	    @CreationTimestamp
	    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	    private LocalDateTime createdAt = LocalDateTime.now();

	    @UpdateTimestamp
	    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	    private LocalDateTime updatedAt;

	    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	    private List<ChatMedia> mediaList; 
	    
	    @Column(nullable = false)
	    private boolean isRead  = false; // Default to false (unread)

    public ChatMessage(String content, User sender, ChatRoom chatRoom, User recipient) {
        this.content = content;
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.recipient = recipient;
        this.isRead = false; // Default to unread
    }

    public ChatMessage() {}

    public int getChatId() {
        return chatId;
    }

    public void setChatId(int chatId) {
        this.chatId = chatId;
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public List<ChatMedia> getMediaList() {
        return mediaList;
    }

    public void setMediaList(List<ChatMedia> mediaList) {
        if (mediaList != null && mediaList.size() > 10) {
            throw new IllegalArgumentException("A chat message cannot have more than 10 media files.");
        }
        this.mediaList = mediaList;
    }
    
    public boolean isRead() {
        return isRead; // Match the field name
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead; // Match the field name
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "chatId=" + chatId +
                ", content='" + content + '\'' +
                ", senderId=" + (sender != null ? sender.getUserId() : "null") +
                ", recipientId=" + (recipient != null ? recipient.getUserId() : "null") +
                ", chatRoomId=" + (chatRoom != null ? chatRoom.getChatRoomId() : "null") +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isRead=" + isRead +
                '}';
    }
}
