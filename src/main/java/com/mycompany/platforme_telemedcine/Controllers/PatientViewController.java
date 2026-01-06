package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.CustomUserDetails;
import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/patient")
public class PatientViewController {

    @Autowired
    private UserRepository userRepository;

    // Helper method to check if user is logged in and has PATIENT role
    private User getAuthenticatedPatient(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        // Get user from database
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return null;
        }

        // Check if user has PATIENT role
        if (!user.getRole().name().equals("PATIENT")) {
            return null;
        }

        return user;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            HttpSession session, Model model) {

        User user = getAuthenticatedPatient(userDetails);
        if (user == null) {
            return "redirect:/login";
        }

        // Store user in session for backward compatibility
        session.setAttribute("user", user);
        session.setAttribute("role", user.getRole());

        model.addAttribute("patient", user);
        return "patient/dashboard";
    }

    @GetMapping("/chat-room")
    public String chatRoom(@AuthenticationPrincipal CustomUserDetails userDetails,
                           HttpSession session, Model model) {

        User user = getAuthenticatedPatient(userDetails);
        if (user == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", user);
        session.setAttribute("role", user.getRole());

        model.addAttribute("patient", user);
        return "patient/chat-room";
    }
}