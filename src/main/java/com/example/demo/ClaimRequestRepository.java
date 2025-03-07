package com.example.demo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimRequestRepository extends JpaRepository<ClaimRequest, Integer> {  
	    List<ClaimRequest> findByUser(User user);
	    
	    @Query("SELECT c FROM ClaimRequest c WHERE c.user.userId = :userId OR c.post.user.userId = :userId")
	    List<ClaimRequest> findClaimRequestsByUser(@Param("userId") int userId);

	    @Query("SELECT c FROM ClaimRequest c WHERE c.post.postId = :postId")
	    Optional<ClaimRequest> findByPost_PostId(@Param("postId") Integer postId);

		Optional<ClaimRequest> findByPost_PostIdAndUser_UserId(int postId, int userId);
	
}
