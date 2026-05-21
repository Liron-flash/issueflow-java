package com.att.tdp.issueflow.services;

import com.att.tdp.issueflow.entities.User;
import com.att.tdp.issueflow.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_PASSWORD = "secret";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public User createUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        String rawPassword = user.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = DEFAULT_PASSWORD;
        }

        user.setPassword(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);
        auditLogService.logUserAction("CREATE", "USER", savedUser.getId());

        return savedUser;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);

        if (updatedUser.getFullName() != null) {
            existingUser.setFullName(updatedUser.getFullName());
        }

        if (updatedUser.getRole() != null) {
            existingUser.setRole(updatedUser.getRole());
        }

        User savedUser = userRepository.save(existingUser);
        auditLogService.logUserAction("UPDATE", "USER", savedUser.getId());

        return savedUser;
    }

    public void deleteUser(Long id) {
        User existingUser = getUserById(id);
        userRepository.delete(existingUser);
        auditLogService.logUserAction("DELETE", "USER", id);
    }
}