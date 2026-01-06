package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User createUser(User user);
    List<User> getAllUsers();
    User getUserById(Long id);
    void deleteUser(Long id);
    User updateUser(User user);
    List<User> getUsersByRole(UserRole role);
    Optional<User> getUserByEmail(String email);

    // Add these methods that are in your implementation
    User getUserByUsername(String name);
    User getUserByPassword(String password);

    // Security methods
    boolean checkPassword(String rawPassword, String encodedPassword);
    User changePassword(Long userId, String newPlainPassword);
    User registerNewUser(String email, String name, String plainPassword, UserRole role);
    User approveUser(Long userId);
    User rejectUser(Long userId, String reason);
    List<User> searchUsers(String keyword);
    Long countByRole(UserRole role);

    // Optional: Method to fix existing passwords
    int encodeAllPlainTextPasswords();
}