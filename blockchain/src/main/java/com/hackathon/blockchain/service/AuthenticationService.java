package com.hackathon.blockchain.service;

import com.hackathon.blockchain.model.User;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final UserService userService;

    public AuthenticationService(UserService userService) {
        this.userService = userService;
    }

    public User authenticate(String username, String password) {
        return userService.findByUsername(username)
            .filter(user -> userService.validatePassword(user, password))
            .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
    }
}
