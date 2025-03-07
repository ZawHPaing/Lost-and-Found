package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import enums.ApprovalStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.security.Principal;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/claimrequests")
public class ClaimRequestController {

    @Autowired
    private ClaimRequestRepository claimRequestRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository messageRepository;
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @GetMapping("/findByPostAndUser/{postId}/{userId}")
    public ResponseEntity<?> getClaimRequestByPostIdAndUserId(@PathVariable("postId") int postId, @PathVariable("userId") int userId) {
        // Fetch the ClaimRequest by postId and userId
        Optional<ClaimRequest> claimRequest = claimRequestRepository.findByPost_PostIdAndUser_UserId(postId, userId);

        if (claimRequest.isPresent()) {
            // Return the found ClaimRequest
        	return ResponseEntity.ok(claimRequest.get().getClaimRequestId());
        } else {
            // Return not found status if no ClaimRequest is found
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Claim request not found for this post and user.");
        }
    }


    // View claim request form
    @GetMapping("/add/{id}")
    public String addClaimRequest(@PathVariable("id") int postId, Model model, HttpSession session) {
        ClaimRequest claimRequest = new ClaimRequest();
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        claimRequest.setPost(post);
        model.addAttribute("claimRequest", claimRequest);
        List<User> users = userRepository.findAll();
	    model.addAttribute("users", users);
	    Long unreadCount = 0L; // Default to 0 if no user is logged in

		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
		   unreadCount = notificationRepository.countByUserAndSeenFalse(loggedInUser);
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnread", unreadCount);
        return "claimRequestForm";
    }

    // View all claim requests
    @GetMapping("/view")
    public String viewClaimRequests(Model model, HttpSession session) {
        // Retrieve user from session
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        Long unreadNoti = 0L;
        Long unreadMsg = 0L; // Default to 0 if no user is logged in
        List<ClaimRequest> claimRequestList = new ArrayList<>();

        if (loggedInUser == null || !loggedInUser.getRole().equals("ROLE_USER")) {
            return "redirect:/access-denied"; // Redirect to access-denied page
        }

        // Fetch all claim requests associated with the logged-in user
        claimRequestList = claimRequestRepository.findClaimRequestsByUser(loggedInUser.getUserId());

        unreadNoti = notificationRepository.countByUserAndSeenFalse(loggedInUser);
        unreadMsg = messageRepository.countTotalUnreadMessagesForUser(loggedInUser.getUserId());
        System.out.println("Unread Noti: " + unreadNoti);
        System.out.println("Unread Messages: " + unreadMsg);

        model.addAttribute("claimRequestList", claimRequestList);
        model.addAttribute("totalUnreadNoti", unreadNoti); 
        model.addAttribute("totalUnreadMsg", unreadMsg); // Always add to model to avoid errors
        return "viewClaimRequests";
    }




    @GetMapping("/viewadmin")
    public String viewClaimRequestsAdmin(Model model,HttpSession session) {
        List<ClaimRequest> claimRequestList = claimRequestRepository.findAll();
        List<Post> pendingPostsAdmin = postRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        model.addAttribute("claimRequestList", claimRequestList);
        Long unreadNoti = 0L;
        Long unreadMsg = 0L; // Default to 0 if no user is logged in

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
        return "viewClaimRequestsAdmin";
    }

    // Save a claim request
    @PostMapping("/save")
    public String saveClaimRequest(ClaimRequest claimRequest,
                            @RequestParam("itemImage") MultipartFile imgFile,
                            @RequestParam("post.postId") int postID,
                            HttpSession session,
                            Model model) {

        String imageName = imgFile.getOriginalFilename();

        // Set the image name in the claim request
        claimRequest.setImgName(imageName);

        // Retrieve user from session
        User loggedInUser = (User) session.getAttribute("loggedInUser");

        if (loggedInUser == null) {
            throw new RuntimeException("No user logged in (session is empty).");
        }

        // Associate the logged-in user
        claimRequest.setUser(loggedInUser);

        // Fetch the existing post
        Post post = postRepository.findById(postID)
                        .orElseThrow(() -> new RuntimeException("Post not found"));
        claimRequest.setPost(post);

        // Save the claim request
        ClaimRequest savedClaimRequest = claimRequestRepository.save(claimRequest);

        // Save the uploaded file
        try {
            String uploadDir = "uploads/claimRequests/" + savedClaimRequest.getClaimRequestId();
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path fileToCreatePath = uploadPath.resolve(imageName);
            Files.copy(imgFile.getInputStream(), fileToCreatePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
     // **Send Notification to Post Owner**
        User postOwner = post.getUser();
        if (postOwner != null) {
            Notification notification = new Notification(
                postOwner,
                loggedInUser,            
                postID,
                "add_claimrequest",
                loggedInUser.getUsername() + " has sent a claim request for your post."
            );
            notificationRepository.save(notification);
        }

        if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
            return "redirect:/claimrequests/viewadmin"; 
        } else {
            return "redirect:/claimrequests/view"; 
        }
    }



    // Delete claim request
    @GetMapping("/delete/{id}")
    public String deleteClaimRequest(@PathVariable("id") int id) {
        claimRequestRepository.deleteById(id);
        return "redirect:/claimrequests/view";
    }

    @GetMapping("/deleteadmin/{id}")
    public String deleteClaimRequestAdmin(@PathVariable("id") int id) {
        claimRequestRepository.deleteById(id);
        return "redirect:/claimrequests/viewadmin";
    }


    @GetMapping("/view/{id}")
	public String viewSingleClaimRequest(@PathVariable("id") Integer id, Model model, HttpSession session) {
		ClaimRequest claimRequest = claimRequestRepository.getReferenceById(id);
		model.addAttribute("claimRequest",claimRequest);
		Long unreadCount = 0L; // Default to 0 if no user is logged in

		// Retrieve user from session
		User loggedInUser = (User) session.getAttribute("loggedInUser");

		if (loggedInUser != null) {
			unreadCount = notificationRepository.countByUserAndSeenFalse(loggedInUser);
			System.out.println("Unread Messages: " + unreadCount);
		} else {
			System.out.println("No user logged in (session is empty).");
		}

		model.addAttribute("totalUnread", unreadCount); // Always add to model to avoid errors
		 if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
	            return "viewSingleClaimRequestAdmin"; 
	        } else {
	            return "viewSingleClaimRequest"; 
	        }

	}
}
