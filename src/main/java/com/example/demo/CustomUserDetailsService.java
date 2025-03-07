package com.example.demo;

import java.util.Optional;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<com.example.demo.User> user = userRepository.findByUsername(username);

        // Check if the user exists
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // Get the user entity
        com.example.demo.User userEntity = user.get();

        // Map the single role to a GrantedAuthority object
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userEntity.getRole());

        // Return a UserDetails object (Spring Security's User class)
        return new User(
            userEntity.getUsername(),
            userEntity.getPassword(),
            Collections.singletonList(authority) // Wrap the single role in a list
        );
    }
}