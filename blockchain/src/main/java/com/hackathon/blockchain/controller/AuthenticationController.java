package com.hackathon.blockchain.controller;

import com.hackathon.blockchain.dto.LoginRequestDto;
import com.hackathon.blockchain.dto.UserRegistrationDto;
import com.hackathon.blockchain.model.User;
import com.hackathon.blockchain.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationService authService;

    public AuthenticationController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationDto request) {
        authService.registerUser(request);
        return ResponseEntity.ok("{\"message\": \"User registered and logged in successfully\"}");
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginRequestDto request) {
        User user = authService.authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("{\"message\": \"Login successful\"}");
    }

    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("You are not authenticated");
        }
        return ResponseEntity.ok("{\"user\": {\"username\": \"" + user.getUsername() + "\"}}");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser() {
        authService.logoutUser();
        return ResponseEntity.ok("{\"message\": \"Logged out successfully\"}");
    }
}
