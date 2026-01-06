package com.mycompany.platforme_telemedcine.config;

import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Initializing Users ===");

        // Create or update admin user with ENCODED password
        createOrUpdateUser("admin@gmail.com", "Admin User", "admin01", UserRole.ADMIN, UserStatus.APPROVED);
        createOrUpdateUser("patient@gmail.com", "Patient User", "patient01", UserRole.PATIENT, UserStatus.APPROVED);
        createOrUpdateUser("doctor@gmail.com", "Medecin User", "doctor123", UserRole.MEDECIN, UserStatus.APPROVED);

        System.out.println("=== Users Ready ===");
        System.out.println("Admin: admin@gmail.com / admin01");
        System.out.println("Patient: patient@gmail.com / patient01");
        System.out.println("Medecin: doctor@gmail.com / doctor123");
    }

    private void createOrUpdateUser(String email, String name, String plainPassword, UserRole role, UserStatus status) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Update existing user
            User user = existingUser.get();
            System.out.println("Updating existing user: " + email);

            // Check if password needs encoding
            String currentPassword = user.getPassword();
            if (currentPassword != null && !currentPassword.startsWith("$2a$") && !currentPassword.startsWith("$2b$")) {
                System.out.println("  Encoding plain password...");
                user.setPassword(passwordEncoder.encode(plainPassword));
            }

            user.setName(name);
            user.setRole(role);
            user.setStatus(status);
            userRepository.save(user);

        } else {
            // Create new user
            User user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setPassword(passwordEncoder.encode(plainPassword)); // ENCODE HERE!
            user.setRole(role);
            user.setStatus(status);

            userRepository.save(user);
            System.out.println("Created new user: " + email);
        }

        // Log the encoded password for reference
        User user = userRepository.findByEmail(email).get();
        System.out.println("  Email: " + user.getEmail());
        System.out.println("  Role: " + user.getRole());
        System.out.println("  Password hash: " + user.getPassword());
        System.out.println("  Hash starts with: " +
                (user.getPassword() != null ? user.getPassword().substring(0, Math.min(10, user.getPassword().length())) : "N/A"));
    }
}