package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.Medecin;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/medecin")
public class MedecinViewController {

    @GetMapping("/dashboard")
    public String medecinDashboard(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        return "medecin/dashboard";
    }

    @GetMapping("/patients")
    public String medecinPatients(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        return "medecin/patients";
    }

    @GetMapping("/patient/{id}")
    public String medecinPatientDetails(@PathVariable Long id, HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        model.addAttribute("patientId", id);
        return "medecin/patient-details";
    }

    @GetMapping("/rendezvous")
    public String medecinRendezVous(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        return "medecin/rendezvous";
    }

    @GetMapping("/consultations")
    public String medecinConsultations(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        return "medecin/consultations";
    }

    @GetMapping("/consultation/{id}")
    public String medecinConsultationRoom(@PathVariable Long id, HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        model.addAttribute("consultationId", id);
        return "medecin/consultation-room";
    }

    @GetMapping("/notifications")
    public String medecinNotifications(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        return "medecin/notifications";
    }

    @GetMapping("/patient/{id}/medical-records")
    public String medecinPatientMedicalRecords(@PathVariable Long id, HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }
        model.addAttribute("medecin", medecin);
        model.addAttribute("patientId", id);
        return "medecin/medical-records";
    }
}