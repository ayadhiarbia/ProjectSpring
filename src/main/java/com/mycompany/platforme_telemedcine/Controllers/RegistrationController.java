package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.*;
import com.mycompany.platforme_telemedcine.Services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Controller
@RequestMapping("/register")
public class RegistrationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedecinRepository medecinRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("userTypes", new String[]{"patient", "doctor"});
        return "register";
    }

    @PostMapping
    public String registerUser(
            @RequestParam String name,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam String userType,
            @RequestParam(required = false) String specialte,
            @RequestParam(required = false) String disponibilite,
            @RequestParam(required = false) String dataNaissance,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String antecedentsMedicaux,
            RedirectAttributes redirectAttributes) {

        // Validation
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/register";
        }

        if (userRepository.findByEmail(email) != null) {
            redirectAttributes.addFlashAttribute("error", "Email already registered!");
            return "redirect:/register";
        }

        try {
            if ("patient".equals(userType)) {
                // Create Patient
                Patient patient = new Patient();
                patient.setName(name);
                patient.setPrenom(prenom);
                patient.setEmail(email);
                patient.setPassword(password); // In production, encrypt this!
                patient.setRole(UserRole.PATIENT);
                patient.setStatus(UserStatus.PENDING);

                // Set additional patient fields
                if (dataNaissance != null && !dataNaissance.isEmpty()) {
                    LocalDate birthDate = LocalDate.parse(dataNaissance);
                    patient.setDataNaissance(Date.from(birthDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                }
                if (adresse != null) patient.setAdresse(adresse);
                if (antecedentsMedicaux != null) patient.setAntecedentsMedicaux(antecedentsMedicaux);

                patientRepository.save(patient);
                emailService.sendRegistrationConfirmation(patient);

            } else if ("doctor".equals(userType)) {
                // Create Doctor
                Medecin doctor = new Medecin();
                doctor.setName(name);
                doctor.setPrenom(prenom);
                doctor.setEmail(email);
                doctor.setPassword(password); // In production, encrypt this!
                doctor.setRole(UserRole.MEDECIN);
                doctor.setStatus(UserStatus.PENDING);

                // Set additional doctor fields
                if (specialte != null) doctor.setSpecialte(specialte);
                if (disponibilite != null) doctor.setDisponibilite(disponibilite);

                medecinRepository.save(doctor);
                emailService.sendRegistrationConfirmation(doctor);
            }

            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Your account is pending administrator approval. " +
                            "You will receive an email once approved.");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }

        return "redirect:/login";
    }
}