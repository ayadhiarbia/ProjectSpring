package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedecinService medecinService;

    @Autowired
    private AdministrateurService adminService;

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "pending", required = false) String pending,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password!");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully!");
        }
        if (pending != null) {
            model.addAttribute("infoMessage", "Your account is pending administrator approval. Please wait for approval email.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "userType", defaultValue = "patient") String userType,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        System.out.println("Login attempt - Email: " + email + ", Type: " + userType);

        switch (userType) {
            case "admin":
                Administrateur admin = adminService.getAdminByEmail(email);
                if (admin != null && admin.getPassword().equals(password)) {
                    setAdminSession(session, admin);
                    redirectAttributes.addFlashAttribute("welcomeMessage", "Welcome back, " + admin.getName() + "!");
                    return "redirect:/admin/dashboard";
                }
                break;

            case "patient":
                Patient patient = patientService.getPatientByEmail(email);
                if (patient != null && patient.getPassword().equals(password)) {
                    // CHECK APPROVAL STATUS
                    if (patient.getStatus() != UserStatus.APPROVED) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                patient.getStatus() == UserStatus.PENDING ?
                                        "Your account is pending administrator approval. Please wait for approval email." :
                                        "Your account is " + patient.getStatus().toString().toLowerCase() + ". Please contact administrator.");
                        return "redirect:/login?pending=true";
                    }
                    setPatientSession(session, patient);
                    return "redirect:/patient/dashboard";
                }
                break;

            case "doctor":
                Medecin medecin = medecinService.getMedecinByEmail(email);
                if (medecin != null && medecin.getPassword().equals(password)) {
                    // CHECK APPROVAL STATUS
                    if (medecin.getStatus() != UserStatus.APPROVED) {
                        redirectAttributes.addFlashAttribute("errorMessage",
                                medecin.getStatus() == UserStatus.PENDING ?
                                        "Your account is pending administrator approval. Please wait for approval email." :
                                        "Your account is " + medecin.getStatus().toString().toLowerCase() + ". Please contact administrator.");
                        return "redirect:/login?pending=true";
                    }
                    setDoctorSession(session, medecin);
                    return "redirect:/medecin/dashboard";
                }
                break;
        }

        // If no user found or password doesn't match
        System.out.println("Login failed for email: " + email);
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid email or password!");
        return "redirect:/login?error=true";
    }

    // Rest of the code remains the same...
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    // Helper methods for setting session attributes
    private void setAdminSession(HttpSession session, Administrateur admin) {
        session.setAttribute("user", admin);
        session.setAttribute("userId", admin.getId());
        session.setAttribute("userName", admin.getName());
        session.setAttribute("userEmail", admin.getEmail());
        session.setAttribute("role", UserRole.ADMIN); // ✅ ENUM
        session.setAttribute("userType", "admin");
        System.out.println("Admin login successful: " + admin.getEmail());
    }

    private void setPatientSession(HttpSession session, Patient patient) {
        session.setAttribute("user", patient);
        session.setAttribute("userId", patient.getId());
        session.setAttribute("userName", patient.getName());
        session.setAttribute("userEmail", patient.getEmail());
        session.setAttribute("role", UserRole.PATIENT); // ✅ ENUM
        session.setAttribute("userType", "patient");
        session.setAttribute("userStatus", patient.getStatus());
        System.out.println("Patient login successful: " + patient.getEmail());
    }

    private void setDoctorSession(HttpSession session, Medecin medecin) {
        session.setAttribute("user", medecin);
        session.setAttribute("userId", medecin.getId());
        session.setAttribute("userName", medecin.getName());
        session.setAttribute("userEmail", medecin.getEmail());
        session.setAttribute("role", UserRole.MEDECIN); // ✅ ENUM
        session.setAttribute("userType", "doctor");
        session.setAttribute("userStatus", medecin.getStatus());
        System.out.println("Doctor login successful: " + medecin.getEmail());
    }
}