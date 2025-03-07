package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/*", "/*/*", "/*/*/*", "/*/*/*/*", "/*/*/*/*/*", "/", "/favicon.ico", "/login", "/register").permitAll()
                // Allow PUT requests to /notifications/mark-all-seen for authenticated users
                .requestMatchers(HttpMethod.PUT, "/notifications/mark-all-seen").authenticated()
                .requestMatchers(HttpMethod.GET, "/chat/room/**/messages").authenticated()
//                .requestMatchers(HttpMethod.POST, "/chat/send").authenticated()
                // Secure all other endpoints
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .disable() // Disable CSRF protection for simplicity (recommended for APIs)
            );

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }
}