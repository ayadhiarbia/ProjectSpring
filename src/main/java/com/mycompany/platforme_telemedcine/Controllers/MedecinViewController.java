package com.mycompany.platforme_telemedcine.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/medecin")
public class MedecinViewController {

    @GetMapping("/dashboard")
    public String medecinDashboard(Model model) {
        return "medecin/dashboard";  // Fixed: was "medecin/Aaathboard"
    }

    @GetMapping("/patient")
    public String medecinPatients() {
        return "medecin/patients";
    }

    @GetMapping("/rendezvous")
    public String medecinRendezVous() {
        return "medecin/rendezvous";
    }

    @GetMapping("/consultations")
    public String medecinConsultations() {
        return "medecin/consultations";
    }

    @GetMapping("/notifications")
    public String medecinNotifications() {
        return "medecin/notifications";
    }
}