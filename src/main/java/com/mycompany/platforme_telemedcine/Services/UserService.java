package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;

import java.util.List;

public interface UserService {
    User createUser(User user);
    List<User> getAllUsers();
    User getUserById(Long id);
    void deleteUser(Long id);
    User updateUser(User user);
    List<User> getUsersByRole(UserRole role);  // Changed to return List
    User getUserByEmail(String email);
}
