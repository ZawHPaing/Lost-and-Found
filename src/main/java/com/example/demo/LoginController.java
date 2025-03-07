package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Display the login page
    @GetMapping("/login")
    public String loginPage(Model model) {
    	List<User> users= userRepository.findAll();
    	model.addAttribute("users", users);
    	return "login";
    }
    
    // Handle login form submission
    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // Find user by username
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            User loggedInUser = user.get();

            // Check if the user is banned
            if (loggedInUser.isBanned()) {
                model.addAttribute("banned", "Your account has been banned.");
                return "redirect:/login?banned=Your account has been banned.";
            }

            // Check if the password matches
            if (passwordEncoder.matches(password, loggedInUser.getPassword())) {
                // Store the logged-in user in the session
                session.setAttribute("loggedInUser", loggedInUser);
                System.out.println("User logged in: " + loggedInUser.getUsername());

                // Add the logged-in user to the model
                model.addAttribute("loggedInUser", loggedInUser);
                
             // Retrieve the user's current warning count
                int warnings = loggedInUser.getWarnings();

                // Retrieve the last warning count that was shown for this user from the session
                Integer lastWarningShown = (Integer) session.getAttribute("lastWarningShown_" + loggedInUser.getUserId());

                // Check if there's a new warning that hasn't been shown yet
                if ((warnings == 1 || warnings == 2) && (lastWarningShown == null || lastWarningShown < warnings)) {
                    String warningMessage = "";

                    if (warnings == 1) {
                        warningMessage = "You have received a warning. Please refrain from inappropriate behavior.";
                    } else if (warnings == 2) {
                        warningMessage = "You have received another warning. You will be banned if you get another warning.";
                    }

                    // Store the warning message in session for the frontend to read
                    session.setAttribute("warningMessage_" + loggedInUser.getUserId(), warningMessage);

                    // Update the session to prevent showing the same warning multiple times
                    session.setAttribute("lastWarningShown_" + loggedInUser.getUserId(), warnings);
                }

                
           
                // Redirect based on user role
                if ("ROLE_ADMIN".equals(loggedInUser.getRole())) {
                	model.addAttribute("loggedInUserId", loggedInUser.getUserId());
                    return "redirect:/posts/managepostsadmin"; 
                } else {
                	model.addAttribute("loggedInUserId", loggedInUser.getUserId());
                    return "redirect:/posts/approvedposts"; 
                }
            } else {
                // Invalid password
                model.addAttribute("error", "Invalid username or password");
                return "redirect:/login?error=Invalid username or password";
            }
        } else {
            // User not found
            model.addAttribute("error", "Invalid username or password");
            return "redirect:/login?error=Invalid username or password";
        }
    }
    // Handle logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Invalidate the session
        return "redirect:/login?logout=true"; // Redirect to login page with logout message
    }
    
    @GetMapping("/access-denied")
    public String accessDenied() {
       
        return "accessDenied"; 
    }
}


