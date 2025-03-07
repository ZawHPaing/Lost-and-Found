package com.example.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
	
	Optional<Notification> findByNotificationId(int notificationId); // Use notificationId

	List<Notification> findByUser(User user);

	@Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.seen = false")
	Long countByUserAndSeenFalse(@Param("user") User user);

	@Query("SELECT n FROM Notification n WHERE n.user = :user AND n.seen = false")
	List<Notification> findByUserAndSeenFalse(@Param("user") User user);

	List<Notification> findByUserOrderByCreatedAtDesc(User user);

	@Transactional
	@Modifying
	@Query("UPDATE Notification n SET n.seen = true WHERE n.referenceId = :chatRoomId AND n.user.userId = :userId AND n.seen = false")
	void markNotificationsAsSeen(@Param("chatRoomId") int chatRoomId, @Param("userId") int userId);
	
	@Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notificationType = :notificationType AND n.seen = false")
	List<Notification> findUnreadNotificationsByType(@Param("user") User user, @Param("notificationType") String notificationType);

}
