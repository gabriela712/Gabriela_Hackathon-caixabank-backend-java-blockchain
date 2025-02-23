package com.hackathon.blockchain.controller;

import com.hackathon.blockchain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @GetMapping("/dashboard")
    public ResponseEntity<String> getUserDashboard(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401)
                .body("You are not authenticated");
        }

        String response = String.format("Welcome to your dashboard, %s! Your registered email is: %s",
            user.getUsername(),
            user.getEmail());

        return ResponseEntity.ok(response);
    }
}