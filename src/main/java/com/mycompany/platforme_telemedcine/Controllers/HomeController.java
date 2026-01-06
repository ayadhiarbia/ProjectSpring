package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @Autowired
    private PatientRepository patientRepository; // Add this

    @GetMapping("/")
    public String home() {
        System.out.println("Home page accessed");
        return "index";
    }

    @GetMapping("/register/patient")
    public String registerPatient() {
        System.out.println("Patient registration page accessed");
        return "auth/register-patient";
    }

    @GetMapping("/register/medecin")
    public String registerMedecin() {
        System.out.println("Doctor registration page accessed");
        return "auth/register-medecin";
    }

    @PostMapping("/register-patient")
    public String processPatientRegistration(
            @RequestParam String name,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String dataNaissance,  // Keep this as String
            @RequestParam String adresse,
            @RequestParam String antecedentsMedicaux,
            @RequestParam String role) {

        System.out.println("✅ PATIENT REGISTRATION SUBMITTED!");
        System.out.println("Name: " + name + " " + prenom);
        System.out.println("Email: " + email);
        System.out.println("DOB: " + dataNaissance);
        System.out.println("Address: " + adresse);
        System.out.println("Role: " + role);
        System.out.println("Medical History: " + antecedentsMedicaux);

        // TODO: Add database saving logic here

        // For now, just redirect to success page

        return "redirect:/registration-success";
    }
    // ⭐⭐⭐ ADD THIS SUCCESS PAGE HANDLER ⭐⭐⭐
    @GetMapping("/registration-success")
    public String registrationSuccess() {
        System.out.println("Registration success page accessed");
        return "auth/registration-success";
    }

    // ⭐⭐⭐ ADD THIS FOR TESTING ⭐⭐⭐
    @GetMapping("/test")
    public String test() {
        System.out.println("Test endpoint accessed");
        return "Test is working!";
    }
}