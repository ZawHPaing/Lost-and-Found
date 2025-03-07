package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {

	@Query("SELECT c FROM ChatRoom c WHERE (c.sender.userId = :user1 AND c.recipient.userId = :user2) OR (c.sender.userId = :user2 AND c.recipient.userId = :user1)")
	Optional<ChatRoom> findByUsers(@Param("user1") Integer user1, @Param("user2") Integer user2);

	@Query("SELECT c FROM ChatRoom c WHERE (c.sender.userId = :userId OR c.recipient.userId = :userId) "
			+ "AND EXISTS (SELECT m FROM ChatMessage m WHERE m.chatRoom = c)")
	List<ChatRoom> findAllByUserId(@Param("userId") Integer userId);

	@Query("SELECT c FROM ChatRoom c WHERE c.sender.userId = :userId OR c.recipient.userId = :userId")
	List<ChatRoom> findByUser(@Param("userId") Integer userId);

}
