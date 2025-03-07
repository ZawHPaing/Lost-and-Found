package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMediaRepository extends JpaRepository<ChatMedia, Integer> {
	List<ChatMedia> findByChatMessage_ChatId(int chatId);
}
