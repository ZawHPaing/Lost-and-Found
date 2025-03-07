package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTasks {
    @Autowired
    private PostService postService;

    // Run the cleanup task every day at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpExpiredPosts() {
        postService.deleteExpiredPosts();
    }
}

