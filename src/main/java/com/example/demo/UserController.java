package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import enums.ApprovalStatus;

@Controller
@RequestMapping("/users")
public class UserController {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;  // Inject password encoder

	    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
	        this.userRepository = userRepository;
	        this.passwordEncoder = passwordEncoder;
	    }


	// Temporary login
	
	/*
	 * @GetMapping("/login") public String loginPage(Model model) { List<User> users
	 * = userRepository.findAll(); model.addAttribute("users", users); return
	 * "login"; }
	 * 
	 * @PostMapping("/login") public String login(@RequestParam int userId,
	 * HttpSession session) { Optional<User> user = userRepository.findById(userId);
	 * 
	 * if (user.isPresent()) { User loggedInUser = user.get();
	 * session.setAttribute("loggedInUser", loggedInUser); // ✅ Store user in
	 * session System.out.println("User logged in: " + loggedInUser.getUserId()); //
	 * ✅ Debug log
	 * 
	 * // Check user role and redirect accordingly if
	 * ("ROLE_ADMIN".equals(loggedInUser.getRole())) { return
	 * "redirect:/posts/managepostsadmin"; } else { return
	 * "redirect:/posts/approvedposts"; } } else { return
	 * "redirect:/users/login?error=User not found"; } }
	 * 
	 * // Logout
	 * 
	 * @GetMapping("/logout") public String logout(HttpSession session) {
	 * session.invalidate(); return "redirect:/users/view"; // Redirect to user list
	 * }
	 */
	 
	/*
	 * @GetMapping("/add") public String addUser(Model model) {
	 * model.addAttribute("user", new User()); // This should be a UserEntity if
	 * you're using JPA return "userForm"; // View name for the form }
	 */

	    @PostMapping("/save")
	    public String saveUser(@Valid User user, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
	        if (bindingResult.hasErrors()) {
	            return "userForm";  // Return to the form if validation fails
	        }

	        // Encode the password before saving
	        user.setPassword(passwordEncoder.encode(user.getPassword()));

	        // Set the role for the user
	        user.setRole("ROLE_USER");

	        // Debug prints (optional, remove for production)
	        System.out.println(user.getUsername());
	        System.out.println(user.getPassword());
	        System.out.println(user.getRole());

	        // Save the user to the database
	        userRepository.save(user);

	        // Add success message and redirect
	        redirectAttributes.addFlashAttribute("success", "User registered!");
	        return "redirect:/users/view";  // Redirect to user view page after saving
	    }
	

	    @GetMapping("/profile/{id}")
	    public String viewProfile(@PathVariable("id") Integer id, Model model) {
	        // Fetch the user by ID
	        User user = userRepository.findById(id)
	            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

	        // Fetch approved posts for the user
	        List<Post> approvedPosts = postRepository.findByUserAndApprovalStatus(user, ApprovalStatus.APPROVED);

	        // Add the user and their approved posts to the model
	        model.addAttribute("user", user);
	        model.addAttribute("postList", approvedPosts);

	        return "profile";
	    }

	@GetMapping("/view")
	public String viewUsers(Model model) {
		model.addAttribute("users", userService.getAllUsers());
		return "viewUsers";
	}

	@GetMapping("/edit/{id}")
	public String showEditForm(@PathVariable int id, Model model) {
		User user = userService.getUserById(id);
		model.addAttribute("user", user);
		return "editUserForm";
	}

	@PostMapping("/updateUser/{id}")
	public String updateUser(@PathVariable("id") Integer id, 
	                         @ModelAttribute User user,
	                         @RequestParam(value = "profileImage", required = false) MultipartFile imgFile,
	                         HttpSession session, RedirectAttributes redirectAttributes) {

	    // Retrieve logged-in user from session
	    User loggedInUser = (User) session.getAttribute("loggedInUser");

	    if (loggedInUser == null) {
	        System.out.println("No user found in session!");
	        return "redirect:/users/login"; // Redirect to login if no user is found
	    }

	    System.out.println("Logged-in User ID: " + loggedInUser.getUserId());

	    // Find the existing user and update it
	    userRepository.findById(id).ifPresent(existingUser -> {
	        existingUser.setUsername(user.getUsername());
	        existingUser.setEmail(user.getEmail());
	        existingUser.setPhoneNumber(user.getPhoneNumber());
	        existingUser.setGender(user.getGender());
	        existingUser.setUpdatedAt(LocalDateTime.now());

	        // Handle password update and encryption only if a new password is provided
	        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
	            // Encrypt the new password using BCrypt
	            String encryptedPassword = passwordEncoder.encode(user.getPassword());
	            existingUser.setPassword(encryptedPassword);
	        }

	        // Handle Profile Image Upload if a new image is provided
	        if (imgFile != null && !imgFile.isEmpty()) {
	            String imageName = imgFile.getOriginalFilename();
	            existingUser.setProfilePicture(imageName);

	            try {
	                String uploadDir = "uploads/users/" + existingUser.getUserId();
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

	        // Save updated user information
	        userRepository.save(existingUser);
	    });

	    return "redirect:/users/profile/{id}"; // Redirect to profile after update
	}


		
	@GetMapping("/delete/{id}")
	public String deleteUser(@PathVariable int id) {
		userService.deleteUser(id);
		return "redirect:/users";
	}

	@GetMapping("/manageUsers")
	public String manageUsers(Model model) {
	    // Fetch only users with the role "ROLE_USER"
	    List<User> users = userService.getUsersByRole("ROLE_USER");
	    List<Post> pendingPostsAdmin = postRepository.findByApprovalStatus(ApprovalStatus.PENDING);
	    model.addAttribute("totalPending", pendingPostsAdmin.size());
	    model.addAttribute("users", users);
	    
	    return "adminUserManagement";
	}


	@PostMapping("/banUser")
	public String banUser(@RequestParam("userId") int userId) {
		userService.banUser(userId);
		return "redirect:manageUsers";
	}

	@PostMapping("/unbanUser")
	public String unbanUser(@RequestParam("userId") int userId) {
		userService.unbanUser(userId);
		return "redirect:manageUsers"; // Redirect to the management page
	}

	@PostMapping("/warnUser")
	public String warnUser(@RequestParam("userId") int userId) {
		userService.warnUser(userId);
		return "redirect:manageUsers";
	}
}