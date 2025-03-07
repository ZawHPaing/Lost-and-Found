package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

public interface ChatRepository extends JpaRepository<ChatMessage, Integer> {
	// Find messages by ChatRoom ID
	List<ChatMessage> findByChatRoom_ChatRoomId(int chatRoomId);

	@Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.recipient.userId = :userId AND c.isRead = false")
	Long countTotalUnreadMessages(@Param("userId") int userId);

	@Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatRoom.chatRoomId = :chatRoomId AND m.recipient.userId = :userId AND m.isRead = false")
	Long countUnreadMessagesForChatRoom(@Param("chatRoomId") int chatRoomId, @Param("userId") int userId);

	@Modifying
	@Transactional // Required for update queries
	@Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatRoom.chatRoomId = :chatRoomId AND m.recipient.userId = :userId AND m.isRead = false")
	void markMessagesAsRead(@Param("chatRoomId") int chatRoomId, @Param("userId") int userId);
	
//	@Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.chatRoomId = :chatRoomId ORDER BY m.createdAt DESC")
//	List<ChatMessage> findLastMessageByChatRoom(@Param("chatRoomId") int chatRoomId, Pageable pageable);
	
	@Query("SELECT COUNT(c) FROM ChatMessage c WHERE c.recipient.userId = :userId AND c.isRead = false")
	Long countTotalUnreadMessagesForUser(@Param("userId") int userId);


}
