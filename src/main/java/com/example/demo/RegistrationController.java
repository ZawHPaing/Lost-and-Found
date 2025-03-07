package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;

import org.springframework.ui.Model; // Correct import
import org.springframework.validation.BindingResult;
@Controller
@RequestMapping("/register")
public class RegistrationController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String showRegistrationForm(@ModelAttribute("user") UserRegistrationDto registrationDto) {
        return "register";
    }

    @PostMapping
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto registrationDto, 
                              BindingResult bindingResult, 
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "register"; // Return to the registration form if there are validation errors
        }

        try {
            userService.saveUser(registrationDto);
            return "redirect:/login";
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Username or email already exists. Please choose a different one.");
            return "register";
        }
    }
}
