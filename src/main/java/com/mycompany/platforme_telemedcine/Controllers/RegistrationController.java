package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.Medecin;
import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Models.UserRole;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import com.mycompany.platforme_telemedcine.Repository.MedecinRepository;
import com.mycompany.platforme_telemedcine.Repository.PatientRepository;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Controller
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedecinRepository medecinRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Show patient registration form
    @GetMapping("/auth/register/patient")
    public String showPatientRegistrationForm(Model model) {
        System.out.println("DEBUG: Showing patient registration form");
        model.addAttribute("userType", "patient");
        return "auth/register-patient";  // Fixed: include "auth/"
    }

    // Show doctor registration form - FIXED
    @GetMapping("/auth/register/medecin")
    public String showDoctorRegistrationForm(Model model) {
        System.out.println("DEBUG: Showing doctor registration form");
        model.addAttribute("userType", "doctor");
        return "auth/register-medecin"; // FIXED: Changed from "auth/register-patient" to "auth/register-medecin"
    }

    // Handle patient registration
    @PostMapping("/auth/register/patient")
    public String registerPatient(
            @RequestParam String name,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String dataNaissance,
            @RequestParam String adresse,
            @RequestParam String antecedentsMedicaux,
            RedirectAttributes redirectAttributes) {

        System.out.println("DEBUG: Registering patient: " + email);

        return processRegistration(name, prenom, email, password, confirmPassword,
                "patient", null, null, dataNaissance, adresse,
                antecedentsMedicaux, redirectAttributes);
    }

    // Handle doctor registration
    @PostMapping("/auth/register/medecin")
    public String registerDoctor(
            @RequestParam String name,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String specialte,
            @RequestParam(required = false) String disponibilite,
            RedirectAttributes redirectAttributes) {

        System.out.println("DEBUG: Registering doctor: " + email);

        return processRegistration(name, prenom, email, password, confirmPassword,
                "doctor", specialte, disponibilite, null, null,
                null, redirectAttributes);
    }

    // Common registration logic
    private String processRegistration(
            String name,
            String prenom,
            String email,
            String password,
            String confirmPassword,
            String userType,
            String specialte,
            String disponibilite,
            String dataNaissance,
            String adresse,
            String antecedentsMedicaux,
            RedirectAttributes redirectAttributes) {

        // --- Validation Step 1: Password Confirmation ---
        if (!password.equals(confirmPassword)) {
            System.out.println("DEBUG: Passwords don't match");
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/auth/register/" + userType;
        }

        // --- Validation Step 2: Check for existing email ---
        if (userRepository.findByEmail(email).isPresent()) {
            System.out.println("DEBUG: Email already exists: " + email);
            redirectAttributes.addFlashAttribute("error", "An account with this email already exists!");
            return "redirect:/auth/register/" + userType;
        }

        try {
            if ("patient".equals(userType)) {
                Patient patient = new Patient();
                patient.setName(name);
                patient.setPrenom(prenom);
                patient.setEmail(email);
                patient.setPassword(passwordEncoder.encode(password));
                patient.setRole(UserRole.PATIENT);
                patient.setStatus(UserStatus.PENDING);

                if (dataNaissance != null && !dataNaissance.isEmpty()) {
                    try {
                        LocalDate birthDate = LocalDate.parse(dataNaissance);
                        patient.setDataNaissance(Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    } catch (Exception e) {
                        System.out.println("DEBUG: Date parsing error: " + e.getMessage());
                    }
                }

                if (adresse != null && !adresse.isEmpty()) {
                    patient.setAdresse(adresse);
                }

                if (antecedentsMedicaux != null && !antecedentsMedicaux.isEmpty()) {
                    patient.setAntecedentsMedicaux(antecedentsMedicaux);
                }

                patientRepository.save(patient);
                System.out.println("DEBUG: Patient saved: " + patient.getId());

            } else if ("doctor".equals(userType)) {
                Medecin doctor = new Medecin();
                doctor.setName(name);
                doctor.setPrenom(prenom);
                doctor.setEmail(email);
                doctor.setPassword(passwordEncoder.encode(password));
                doctor.setRole(UserRole.MEDECIN);
                doctor.setStatus(UserStatus.PENDING);

                if (specialte != null && !specialte.isEmpty()) {
                    doctor.setSpecialte(specialte);
                }

                if (disponibilite != null && !disponibilite.isEmpty()) {
                    doctor.setDisponibilite(disponibilite);
                }

                medecinRepository.save(doctor);
                System.out.println("DEBUG: Doctor saved: " + doctor.getId());

            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid user type selected.");
                return "redirect:/";
            }

            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Your account is pending administrator approval.");

            // Redirect to login page after successful registration
            return "redirect:/login?registered=true";

        } catch (Exception e) {
            System.out.println("DEBUG: Registration ERROR: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "An unexpected error occurred. Please try again: " + e.getMessage());
            return "redirect:/auth/register/" + userType;
        }
    }

    // Add registration success page
    @GetMapping("/auth/registration-success")
    public String showRegistrationSuccess(Model model) {
        System.out.println("DEBUG: Showing registration success page");
        return "auth/registration-success";
    }
}