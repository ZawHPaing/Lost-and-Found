package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

import java.util.Optional;

import org.springframework.web.bind.annotation.*;

import enums.ApprovalStatus;
import jakarta.servlet.http.HttpSession;

@Controller

public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ChatRepository messageRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private PostService postService;

    // View the post form
    @GetMapping("posts/add")
    public String addPost(Model model, HttpSession session) {
        // Retrieve user from session
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        Post newPost = new Post(); // Create a new Post object

        if (loggedInUser != null) {
            newPost.setUser(loggedInUser); // Set the post's User object
            System.out.println("Logged-in User ID: " + loggedInUser.getUserId());
        } else {
            System.out.println("No user logged in (session is empty).");
        }

        model.addAttribute("post", newPost); // Add the post object to the model
        List<User> users = userRepository.findAll(); // Fetch users
        model.addAttribute("users", users);
        

        Long unreadNoti = 0L; 
        Long unreadMsg = 0L; // Default to 0 if no user is logged in
        unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
		unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
		System.out.println("Unread Noti: " + unreadNoti);
		System.out.println("Unread Messages: " + unreadMsg);
        
		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);

        return "postForm"; // View name
    }

    
    // View all posts
    @GetMapping("posts/view")
    public String viewPosts(Model model,HttpSession session) {
        List<Post> postList = postRepository.findAll();
        model.addAttribute("postList", postList);
        Long unreadNoti = 0L;
        Long unreadMsg = 0L; // Default to 0 if no user is logged in

		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti); 
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "viewPosts";
    }
    
    @PostMapping("posts/save")
    public String savePost(@ModelAttribute Post post, 
                            @RequestParam("itemImage") MultipartFile imgFile, 
                            Model model, HttpSession session) {
        // Retrieve logged-in user from session
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            System.out.println("No user found in session!");
            return "redirect:/users/login"; // Redirect to login if no user is found
        }

        System.out.println("Logged-in User ID: " + loggedInUser.getUserId());

        // Set the user for the post
        post.setUser(loggedInUser);

        // Handle image upload
        String imageName = imgFile.getOriginalFilename();
        post.setImgName(imageName);

        // Save the post to get the postId
        Post savedPost = postRepository.save(post);

        try {
            String uploadDir = "uploads/posts/" + savedPost.getPostId();
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path fileToCreatePath = uploadPath.resolve(imageName);
            System.out.println("File Path: " + fileToCreatePath);

            Files.copy(imgFile.getInputStream(), fileToCreatePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "redirect:/posts/pendingposts";
    }

    
    @PostMapping("/posts/update")
    public String updatePost(@ModelAttribute Post post, 
                             @RequestParam(value = "itemImage", required = false) MultipartFile imgFile, 
                             Model model, HttpSession session) {

        // Retrieve logged-in user from session
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            System.out.println("No user found in session!");
            return "redirect:/users/login"; // Redirect to login if no user is found
        }

        System.out.println("Logged-in User ID: " + loggedInUser.getUserId());

        // Find the existing post and update it
        postRepository.findById(post.getPostId()).ifPresent(existingPost -> {
            existingPost.setTitle(post.getTitle());
            existingPost.setDescription(post.getDescription());
            existingPost.setPostType(post.getPostType());
            existingPost.setCategory(post.getCategory());
            existingPost.setLocation(post.getLocation());
            existingPost.setDate(post.getDate());
            existingPost.setApprovalStatus(ApprovalStatus.PENDING);
            existingPost.setUpdatedAt(LocalDateTime.now());
            
            // Ensure the user remains the same
            existingPost.setUser(loggedInUser);

            // Handle Image Upload if a new image is provided
            if (imgFile != null && !imgFile.isEmpty()) {
                String imageName = imgFile.getOriginalFilename();
                existingPost.setImgName(imageName);

                try {
                    String uploadDir = "uploads/posts/" + existingPost.getPostId();
                    Path uploadPath = Paths.get(uploadDir);
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                    }

                    Path filePath = uploadPath.resolve(imageName);
                    Files.copy(imgFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            postRepository.save(existingPost);
        });

        return "redirect:/posts/manageposts";
    }


    
    // Search by keyword
    @GetMapping("posts/search")
    public String searchPosts(@RequestParam("query") String query, Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchFullText(query);
        model.addAttribute("postList", postList);
        model.addAttribute("query", query);
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "viewPosts";
    }

    // Search by date
    @GetMapping("posts/searchByDate")
    public String searchPostsByDate(@RequestParam(value = "date", required = false) String date, 
                                    Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate the date input
        if (date == null || date.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }

        // Fetch posts based on the selected date
        List<Post> postList = postRepository.searchByDate(date);
        model.addAttribute("postList", postList);
        model.addAttribute("date", date);

        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "viewPosts";
    }


    // Search by category
    @GetMapping("posts/searchByCategory")
    public String searchPostsByCategory(@RequestParam("category") String category, Model model,HttpSession session, RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchByCategory(category);
        model.addAttribute("postList", postList);
        model.addAttribute("category", category);
        
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "viewPosts";
    }
    
    @GetMapping("posts/searchPending")
    public String searchPostsPending(@RequestParam("query") String query, Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchFullText(query);
        model.addAttribute("postList", postList);
        model.addAttribute("query", query);
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "pendingPosts";
    }

    // Search by date
    @GetMapping("posts/searchByDatePending")
    public String searchPostsByDatePending(@RequestParam(value = "date", required = false) String date, 
                                    Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate the date input
        if (date == null || date.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }

        // Fetch posts based on the selected date
        List<Post> postList = postRepository.searchByDate(date);
        model.addAttribute("postList", postList);
        model.addAttribute("date", date);

        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "pendingPosts";
    }


    // Search by category
    @GetMapping("posts/searchByCategoryPending")
    public String searchPostsByCategoryPending(@RequestParam("category") String category, Model model,HttpSession session, RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchByCategory(category);
        model.addAttribute("postList", postList);
        model.addAttribute("category", category);
        
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "pendingPosts";
    }
    
    @GetMapping("posts/searchPendingAdmin")
    public String searchPostsPendingAdmin(@RequestParam("query") String query, Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchFullText(query);
        model.addAttribute("postList", postList);
        model.addAttribute("query", query);
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "pendingPostsAdmin";
    }

    // Search by date
    @GetMapping("posts/searchByDatePendingAdmin")
    public String searchPostsByDatePendingAdmin(@RequestParam(value = "date", required = false) String date, 
                                    Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate the date input
        if (date == null || date.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }

        // Fetch posts based on the selected date
        List<Post> postList = postRepository.searchByDate(date);
        model.addAttribute("postList", postList);
        model.addAttribute("date", date);

        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "pendingPostsAdmin";
    }


    // Search by category
    @GetMapping("posts/searchByCategoryPendingAdmin")
    public String searchPostsByCategoryPendingAdmin(@RequestParam("category") String category, Model model,HttpSession session, RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchByCategory(category);
        model.addAttribute("postList", postList);
        model.addAttribute("category", category);
        
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "pendingPostsAdmin";
    }
    
    @GetMapping("posts/searchManage")
    public String searchPostsManage(@RequestParam("query") String query, Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchFullText(query);
        model.addAttribute("postList", postList);
        model.addAttribute("query", query);
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "managePosts";
    }

    // Search by date
    @GetMapping("posts/searchByDateManage")
    public String searchPostsByDateManage(@RequestParam(value = "date", required = false) String date, 
                                    Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate the date input
        if (date == null || date.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }

        // Fetch posts based on the selected date
        List<Post> postList = postRepository.searchByDate(date);
        model.addAttribute("postList", postList);
        model.addAttribute("date", date);

        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "managePosts";
    }


    // Search by category
    @GetMapping("posts/searchByCategoryManage")
    public String searchPostsByCategoryManage(@RequestParam("category") String category, Model model,HttpSession session, RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchByCategory(category);
        model.addAttribute("postList", postList);
        model.addAttribute("category", category);
        
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "managePosts";
    }
    
    @GetMapping("posts/searchManageAdmin")
    public String searchPostsManageAdmin(@RequestParam("query") String query, Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchFullText(query);
        model.addAttribute("postList", postList);
        model.addAttribute("query", query);
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/managepostsadmin"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);
        return "managePostsAdmin";
    }

    // Search by date
    @GetMapping("posts/searchByDateManageAdmin")
    public String searchPostsByDateManageAdmin(@RequestParam(value = "date", required = false) String date, 
                                    Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate the date input
        if (date == null || date.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date.");
            return "redirect:/posts/managepostsadmin"; // Redirect to the main posts page
        }

        // Fetch posts based on the selected date
        List<Post> postList = postRepository.searchByDate(date);
        model.addAttribute("postList", postList);
        model.addAttribute("date", date);

        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "managePostsAdmin";
    }


    // Search by category
    @GetMapping("posts/searchByCategoryManageAdmin")
    public String searchPostsByCategoryManageAdmin(@RequestParam("category") String category, Model model,HttpSession session, RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchByCategory(category);
        model.addAttribute("postList", postList);
        model.addAttribute("category", category);
        
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/managepostsadmin"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "managePostsAdmin";
    }
    
    @GetMapping("posts/searchRejectedAdmin")
    public String searchPostsRejectedAdmin(@RequestParam("query") String query, Model model,HttpSession session,RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchFullText(query);
        model.addAttribute("postList", postList);
        model.addAttribute("query", query);
        
        if (query == null || query.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "rejectedPostsAdmin";
    }

    // Search by date
    @GetMapping("posts/searchByDateRejectedAdmin")
    public String searchPostsByDateRejectedAdmin(@RequestParam(value = "date", required = false) String date, 
                                    Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        // Validate the date input
        if (date == null || date.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a valid date.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }

        // Fetch posts based on the selected date
        List<Post> postList = postRepository.searchByDate(date);
        model.addAttribute("postList", postList);
        model.addAttribute("date", date);

        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "rejectedPostsAdmin";
    }


    // Search by category
    @GetMapping("posts/searchByCategoryRejectedAdmin")
    public String searchPostsByCategoryRejectedAdmin(@RequestParam("category") String category, Model model,HttpSession session, RedirectAttributes redirectAttributes) {
        List<Post> postList = postRepository.searchByCategory(category);
        model.addAttribute("postList", postList);
        model.addAttribute("category", category);
        
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a category.");
            return "redirect:/posts/approvedposts"; // Redirect to the main posts page
        }
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "rejectedPostsAdmin";
    }
    
    @GetMapping("/posts/view/{id}")
	public String viewSinglePost(@PathVariable("id") Integer id, Model model,HttpSession session) {
		Post post = postRepository.getReferenceById(id);
		model.addAttribute("post",post);
		Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
		 if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
	            return "viewSinglePostAdmin"; 
	        } else {
	            return "viewSinglePost"; 
	        } 
			
	}
    
    @GetMapping("posts/pendingposts")
    public String viewPendingPosts(Model model, HttpSession session) {
    	
    	Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 

        List<Post> pendingPosts = new ArrayList<>(); // Initialize empty list

        // Retrieve user from session
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_USER")) {
            return "redirect:/access-denied"; // Redirect to access-denied page
        }
        	unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
        	unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);

            // Fetch only posts belonging to the logged-in user
            pendingPosts = postRepository.findByUserAndApprovalStatus(loggedInUser, ApprovalStatus.PENDING);

        model.addAttribute("postList", pendingPosts);
        model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "pendingPosts";
    }

    
    @GetMapping("posts/pendingpostsadmin")
    public String viewPendingPostsAdmin(Model model,HttpSession session) {
    	List<Post> pendingPostsAdmin = postRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        model.addAttribute("postList", pendingPostsAdmin);
        System.out.println("Pending post count: " + pendingPostsAdmin.size());
        model.addAttribute("totalPending", pendingPostsAdmin.size());
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		 if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_ADMIN")) {
		        return "redirect:/access-denied"; // Redirect to access-denied page
		    }
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		
		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "pendingPostsAdmin";
    }
    
    
    @GetMapping("posts/approvedposts")
    public String viewApprovedPosts(Model model, HttpSession session) {
    	List<Post> approvedPosts = postRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        model.addAttribute("postList", approvedPosts);
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		 if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_USER")) {
		        return "redirect:/access-denied"; // Redirect to access-denied page
		    }
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
		return "viewPosts";
    }
    
    @GetMapping("posts/rejectedpostsadmin")
    public String viewRejectedPosts(Model model,HttpSession session) {
    	List<Post> rejectedPosts = postRepository.findByApprovalStatus(ApprovalStatus.REJECTED);
    	List<Post> pendingPostsAdmin = postRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        model.addAttribute("postList", rejectedPosts);
        model.addAttribute("totalPending", pendingPostsAdmin.size());
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		 if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_ADMIN")) {
		        return "redirect:/access-denied"; // Redirect to access-denied page
		    }
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "rejectedPostsAdmin";
    }
    
    @GetMapping("posts/approvedpostsadmin")
    public String viewApprovedPostsAdmin(Model model,HttpSession session) {
    	List<Post> approvedPosts = postRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
        model.addAttribute("postList", approvedPosts);
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		 if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_ADMIN")) {
		        return "redirect:/access-denied"; // Redirect to access-denied page
		    }
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "viewPostsAdmin";
    }
    
    @GetMapping("posts/managepostsadmin")
    public String managePostsAdmin(Model model,HttpSession session) {
    	List<Post> managePostsAdmin = postRepository.findByApprovalStatus(ApprovalStatus.APPROVED);
    	List<Post> pendingPostsAdmin = postRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        model.addAttribute("postList", managePostsAdmin);
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");
		
		 if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_ADMIN")) {
		        return "redirect:/access-denied"; // Redirect to access-denied page
		    }
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		
		model.addAttribute("totalPending", pendingPostsAdmin.size());
		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "managePostsAdmin";
    }
    
    @GetMapping("/posts/pending/{postId}")
    public String pendingPost(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setApprovalStatus(ApprovalStatus.APPROVED);
            post.setUpdatedAt(LocalDateTime.now());
            postRepository.save(post);
        });
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "redirect:/posts/pendingpostsadmin";
    }
    
    @GetMapping("posts/manageposts")
    public String managePosts(Model model,HttpSession session) {
    	List<Post> managePosts = new ArrayList<>();
        
    	Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 

		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		 if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_USER")) {
		        return "redirect:/access-denied"; // Redirect to access-denied page
		    }
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
			managePosts = postRepository.findByUser(loggedInUser);
		
		model.addAttribute("postList", managePosts);

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "managePosts";
    }
    
    
    
    @GetMapping("/posts/approve/{postId}")
    public String approvePost(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
    	 postRepository.findById(postId).ifPresent(post -> {
             post.setApprovalStatus(ApprovalStatus.APPROVED);
             post.setUpdatedAt(LocalDateTime.now());
             postRepository.save(post);
             postService.updateApprovalStatus(postId, ApprovalStatus.APPROVED);

             if (post.getUser() != null) {
                 User postOwner = post.getUser();

              // Create and save notification
                 Notification notification = new Notification(
                     postOwner,  
                     null,
                     post.getPostId(),
                     "post_approved",  
                     "Your post has been approved!"  
                 );
                 notificationRepository.save(notification);
             }
         });

        return "redirect:/posts/pendingpostsadmin";
    }
    
    @GetMapping("/posts/reject/{postId}")
    public String rejectPost(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setApprovalStatus(ApprovalStatus.REJECTED);
            post.setUpdatedAt(LocalDateTime.now());
            postRepository.save(post);
        });
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "redirect:/posts/pendingpostsadmin";
    }
    
    @GetMapping("/posts/restore/{postId}")
    public String restorePost(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setApprovalStatus(ApprovalStatus.PENDING);
            post.setUpdatedAt(LocalDateTime.now());
            postRepository.save(post);
        });
        return "redirect:/posts/rejectedpostsadmin";
    }
    
    @GetMapping("/posts/delete/{postId}")
    public String deletePost(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
        postRepository.findById(postId).ifPresent(post -> {
            postRepository.delete(post);  // Delete the post from the repository
        });
        Long unreadNoti = 0L;
        Long unreadMsg = 0L;// Default to 0 if no user is logged in

		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "redirect:/posts/manageposts";  // Redirect to the list of posts after deletion
    }
    
    @GetMapping("/posts/deleteadmin/{postId}")
    public String deletePostAdmin(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
        postRepository.findById(postId).ifPresent(post -> {
            postRepository.delete(post);  // Delete the post from the repository
        });
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "redirect:/posts/managepostsadmin";  // Redirect to the list of posts after deletion
    }
    
    @GetMapping("/posts/rejecteddeleteadmin/{postId}")
    public String RejectedDeletePostAdmin(@PathVariable int postId, RedirectAttributes redirectAttributes,Model model,HttpSession session) {
        postRepository.findById(postId).ifPresent(post -> {
            postRepository.delete(post);  // Delete the post from the repository
        });
        Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
        return "redirect:/posts/rejectedpostsadmin";  // Redirect to the list of posts after deletion
    }
    
    @GetMapping("/posts/edit/{postId}")
    public String editPost(@PathVariable int postId, Model model,HttpSession session) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        model.addAttribute("post", post);
        List<User> userList = userRepository.findAll(); // Fetch categories
	    model.addAttribute("userList", userList);
	    Long unreadNoti = 0L; // Default to 0 if no user is logged in
        Long unreadMsg = 0L; 
		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
			System.out.println("Unread Noti: " + unreadNoti);
			System.out.println("Unread Messages: " + unreadMsg);
		        
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnreadNoti", unreadNoti);
		model.addAttribute("totalUnreadMsg", unreadMsg);// Always add to model to avoid errors
		
		postService.updateApprovalStatus(postId, ApprovalStatus.PENDING);
        return "editSinglePost";
    }
    
    


    // Provide predefined categories for the dropdown
    @ModelAttribute("categories")
    public List<String> getCategories() {
        return Arrays.asList(
            "Books", "Electronics", "Clothing", "Stationery", 
            "Sports Equipment", "Musical Instruments", 
            "ID Cards", "Accessories", "Lab Equipment", 
            "Personal Belongings", "Others"
        );
    }
}
