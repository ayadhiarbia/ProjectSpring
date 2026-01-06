package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import com.mycompany.platforme_telemedcine.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImp implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;  // CRITICAL: Add this!

    @Override
    public User createUser(User user) {
        // ENCODE PASSWORD BEFORE SAVING
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
        }

        // Set default values
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.PENDING);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }

        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public User updateUser(User user) {
        // Get existing user to preserve encoded password if not changing
        User existingUser = userRepository.findById(user.getId()).orElse(null);
        if (existingUser == null) {
            return null;
        }

        // Only encode password if it's being changed (and not already encoded)
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            String currentPassword = user.getPassword();

            // Check if it's already encoded (BCrypt hash)
            boolean isAlreadyEncoded = currentPassword.startsWith("$2a$") ||
                    currentPassword.startsWith("$2b$");

            if (!isAlreadyEncoded) {
                // It's plain text, encode it
                String encodedPassword = passwordEncoder.encode(currentPassword);
                user.setPassword(encodedPassword);
            }
        } else {
            // Keep existing encoded password
            user.setPassword(existingUser.getPassword());
        }

        // Preserve other fields if not provided
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(existingUser.getCreatedAt());
        }
        if (user.getStatus() == null) {
            user.setStatus(existingUser.getStatus());
        }

        return userRepository.save(user);
    }

    @Override
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getUserByUsername(String name) {
        return userRepository.findByName(name);
    }

    @Override
    public User getUserByPassword(String password) {
        // WARNING: This is insecure and inefficient!
        // Searching by password (especially encoded) is not recommended
        return userRepository.findByPassword(password);
    }

    // === ADDITIONAL SECURITY METHODS ===

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public User changePassword(Long userId, String newPlainPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        String encodedPassword = passwordEncoder.encode(newPlainPassword);
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }

    public User registerNewUser(String email, String name, String plainPassword, UserRole role) {
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(plainPassword)); // ENCODED!
        user.setRole(role);
        user.setStatus(UserStatus.PENDING);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public User approveUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        user.setStatus(UserStatus.APPROVED);
        user.setApprovedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User rejectUser(Long userId, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        user.setStatus(UserStatus.REJECTED);
        user.setRejectionReason(reason);
        return userRepository.save(user);
    }

    public List<User> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword);
    }

    public Long countByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    @Override
    public int encodeAllPlainTextPasswords() {
        return 0;
    }
}