package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.Patient;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/patient")
public class PatientViewController {

    @GetMapping("/dashboard")
    public String patientDashboard(HttpSession session, Model model) {
        // Check if patient is logged in
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            System.out.println("No patient in session, redirecting to login");
            return "redirect:/login";
        }

        System.out.println("Loading dashboard for patient: " + patient.getEmail());
        model.addAttribute("patient", patient);
        return "patient/dashboard";
    }

    @GetMapping("/profile/{id}")
    public String patientProfile(@PathVariable Long id, Model model) {
        model.addAttribute("patientId", id);
        return "patient/profile";
    }

    @GetMapping("/rendezvous")
    public String patientRendezVous(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "patient/rendezvous";
    }

    @GetMapping("/dossier")
    public String patientDossier(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "patient/dossier";
    }

    @GetMapping("/chat")
    public String patientChat(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        return "patient/chat";
    }
}