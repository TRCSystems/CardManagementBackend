package com.cardsystem.services;

import com.cardsystem.dto.RegisterRequest;
import com.cardsystem.models.User;
import com.cardsystem.models.constants.UserRole;
import com.cardsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(RegisterRequest req) {
        Optional<User> existing = userRepository.findByUsername(req.getUsername());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("username already taken");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setRole(UserRole.valueOf(req.getRole()));
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("old password does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void changeRole(String username, String newRole) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("user not found"));
        user.setRole(UserRole.valueOf(newRole));
        userRepository.save(user);
    }


    public java.util.List<com.cardsystem.dto.UserResponse> listAllUsers() {
        return userRepository.findAll().stream().map(u ->
                com.cardsystem.dto.UserResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .fullName(u.getFullName())
                        .role(u.getRole())
                        .active(u.isActive())
                        .createdAt(u.getCreatedAt())
                        .updatedAt(u.getUpdatedAt())
                        .build()
        ).toList();
    }
}
