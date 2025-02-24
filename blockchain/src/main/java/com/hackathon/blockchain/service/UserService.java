package com.hackathon.blockchain.service;

import com.hackathon.blockchain.model.User;
import com.hackathon.blockchain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String email, String password) {
        System.out.println("Intentando guardar usuario: " + username + " - " + email);
    
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
    
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
    
        User savedUser = userRepository.save(user);
        System.out.println("Usuario guardado con ID: " + savedUser.getId());
    
        return savedUser;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean validatePassword(User user, String password) {
        // Verifica si la contraseña en texto plano coincide con el hash almacenado
        return passwordEncoder.matches(password, user.getPassword());
    }
}