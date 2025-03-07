package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import enums.ApprovalStatus;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    private final PostRepository postRepository;

    @Autowired
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public List<Post> searchItems(String query) {
        return postRepository.searchFullText(query);
    }
    
    public void deleteExpiredPosts() {
        postRepository.deleteExpiredPosts();
    }
    
    @Transactional
    public void updateApprovalStatus(int postId, ApprovalStatus newStatus) {
        Optional<Post> postOptional = postRepository.findById(postId);

        if (postOptional.isPresent()) {
            Post post = postOptional.get();
            post.setApprovalStatus(newStatus);

            // If status is set to APPROVED, start expiry timer (e.g., 30 days from now)
            if (newStatus == ApprovalStatus.APPROVED) {
                LocalDateTime expiryDate = LocalDateTime.now().plusDays(30);
                post.setExpiryDate(expiryDate);
                postRepository.updateExpiryDate(postId, expiryDate);
            }

            postRepository.save(post);
        } else {
            throw new RuntimeException("Post not found");
        }
    }
}

    


