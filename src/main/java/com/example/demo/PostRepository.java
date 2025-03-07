package com.example.demo;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import enums.ApprovalStatus;
import jakarta.transaction.Transactional;

public interface PostRepository extends JpaRepository<Post, Integer> {

    // Full-text search by keyword
    @Query(value = "SELECT * FROM posts WHERE MATCH(category, description, location, title) AGAINST(:query IN NATURAL LANGUAGE MODE)", nativeQuery = true)
    List<Post> searchFullText(@Param("query") String query);

    // Search by date
    @Query(value = "SELECT * FROM posts WHERE DATE(date) = :date", nativeQuery = true)
    List<Post> searchByDate(@Param("date") String date);

    // Search by category
    @Query(value = "SELECT * FROM posts WHERE category = :category", nativeQuery = true)
    List<Post> searchByCategory(@Param("category") String category);
    
    // Fetch posts by approval status and ensure they are not expired
    @Query("SELECT p FROM Post p WHERE p.approvalStatus = :status AND p.expiryDate > CURRENT_TIMESTAMP")
    List<Post> findByApprovalStatus(@Param("status") ApprovalStatus status);
    
    // Fetch posts by user, ensuring they are not expired
    @Query("SELECT p FROM Post p WHERE p.user = :user AND p.expiryDate > CURRENT_TIMESTAMP")
    List<Post> findByUser(@Param("user") User user);

    // Fetch posts by user and approval status, ensuring they are not expired
    
        List<Post> findByUserAndApprovalStatus(User user, ApprovalStatus approvalStatus);
    
    
    @Transactional
    @Modifying
    @Query("UPDATE Post p SET p.expiryDate = :expiryDate WHERE p.id = :postId")
    void updateExpiryDate(@Param("postId") int postId, @Param("expiryDate") LocalDateTime expiryDate);

    
    // Delete expired posts
    @Transactional
    @Modifying
    @Query("DELETE FROM Post p WHERE p.expiryDate <= CURRENT_TIMESTAMP")
    void deleteExpiredPosts();



}

    
    


