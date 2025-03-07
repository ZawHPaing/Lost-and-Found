package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void saveUser(UserRegistrationDto registrationDto) {
        // Check if email already exists
        if (userRepository.findByEmail(registrationDto.getEmail()) != null) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail()); 
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(registrationDto.getRole()); // Set the role to ROLE_USER
        userRepository.save(user);
    }



    
    public User getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User with ID " + id + " not found"));
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUser(User updatedUser) {
        User existingUser = userRepository.findById(updatedUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + updatedUser.getUserId()));

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());  
        
        if (existingUser.getRole() == "ROLE_USER") {
            
            
            existingUser.setGender(updatedUser.getGender());

            existingUser.setPhoneNumber(updatedUser.getPhoneNumber());  
        } else {
           
            if (updatedUser.getGender() != null) existingUser.setGender(updatedUser.getGender());
            
        }

        // Update password only if it's not empty
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(updatedUser.getPassword()); // Store password as plain text
        }

        userRepository.save(existingUser);
    }

    public void deleteUser(int id) {
        userRepository.deleteById(id);
    }
    
    public void banUser(int userId) {
        User user = getUserById(userId);
        user.setBanned(true);
        userRepository.save(user);
    }

    public void unbanUser(int userId) {
        User user = getUserById(userId);
        user.setWarnings(0);
        ((User) user).setBanned(false);
        userRepository.save(user);
    }

    
    public void warnUser(int userId) {
        User user = getUserById(userId);
        user.setWarnings(user.getWarnings() + 1);
        if (user.getWarnings() >= 3) {
            user.setBanned(true);
            System.out.println("User " + user.getUsername() + " has been banned after 3 warnings.");
        }

        userRepository.save(user);
    }
    
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role); // assuming you have this method in your repository
    }

}
