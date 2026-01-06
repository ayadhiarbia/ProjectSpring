package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.MedecinService;
import com.mycompany.platforme_telemedcine.Services.RendezVousService;
import com.mycompany.platforme_telemedcine.Services.ConsultationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/medecin")
public class MedecinViewController {

    private final RendezVousService rendezVousService;
    private final ConsultationService consultationService;
    private final MedecinService medecinService;

    // Constructor injection
    public MedecinViewController(RendezVousService rendezVousService,
                                 ConsultationService consultationService,
                                 MedecinService medecinService) {
        this.rendezVousService = rendezVousService;
        this.consultationService = consultationService;
        this.medecinService = medecinService;
    }

    @GetMapping("/dashboard")
    public String medecinDashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpSession session, Model model) {

        // Check if user is authenticated
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // Get the medecin by ID from the authenticated user
            Long medecinId = userDetails.getUserId();
            Medecin medecin = medecinService.getMedecinById(medecinId);

            if (medecin == null) {
                return "redirect:/login?error=userNotFound";
            }

            // Store in session for future use in this controller
            session.setAttribute("user", medecin);

            // Get all appointments for this doctor
            List<RendezVous> allAppointments = rendezVousService.getRendezVousByMedecinId(medecinId);

            // Calculate statistics
            int totalAppointments = allAppointments.size();
            int pendingCount = (int) allAppointments.stream()
                    .filter(rv -> rv.getStatus() == StatusRendezVous.PENDING)
                    .count();
            int completedCount = (int) allAppointments.stream()
                    .filter(rv -> rv.getStatus() == StatusRendezVous.COMPLETED)
                    .count();

            LocalDate today = LocalDate.now();
            LocalDate nextWeek = today.plusDays(7);

            int next7DaysCount = (int) allAppointments.stream()
                    .filter(rv -> {
                        if (rv.getDate() == null) return false;
                        LocalDate rdvDate = rv.getDate();
                        return !rdvDate.isBefore(today) && rdvDate.isBefore(nextWeek);
                    })
                    .count();

            int actionRequired = (int) allAppointments.stream()
                    .filter(rv -> rv.getStatus() == StatusRendezVous.PENDING)
                    .count();

            int auditCount = 0;

            // Get consultations count
            List<Consultation> allConsultations = consultationService.getAllConsultations();
            int consultationCount = (int) allConsultations.stream()
                    .filter(c -> {
                        RendezVous rdv = c.getRendezVous();
                        return rdv != null &&
                                rdv.getMedecin() != null &&
                                rdv.getMedecin().getId().equals(medecinId);
                    })
                    .count();

            // Get active patients (patients with appointments in last 30 days)
            LocalDate thirtyDaysAgo = today.minusDays(30);
            long activePatients = allAppointments.stream()
                    .filter(rv -> {
                        if (rv.getDate() == null || rv.getPatient() == null) return false;
                        LocalDate rdvDate = rv.getDate();
                        return !rdvDate.isBefore(thirtyDaysAgo);
                    })
                    .map(rv -> rv.getPatient().getId())
                    .distinct()
                    .count();

            // Get upcoming appointments for the next 7 days for the table
            List<RendezVous> upcomingAppointments = allAppointments.stream()
                    .filter(rv -> {
                        if (rv.getDate() == null) return false;
                        LocalDate rdvDate = rv.getDate();
                        return !rdvDate.isBefore(today) && rdvDate.isBefore(nextWeek);
                    })
                    .sorted((rv1, rv2) -> {
                        if (rv1.getDate() == null || rv2.getDate() == null) return 0;
                        return rv1.getDate().compareTo(rv2.getDate());
                    })
                    .collect(Collectors.toList());

            // Add all data to model
            model.addAttribute("medecin", medecin);

            // Task stats
            model.addAttribute("totalAppointments", totalAppointments);
            model.addAttribute("pendingCount", pendingCount);
            model.addAttribute("auditCount", auditCount);

            // Toolkit stats
            model.addAttribute("actionRequired", actionRequired);
            model.addAttribute("upcomingCount", next7DaysCount);
            model.addAttribute("completedCount", completedCount);

            // User stats
            model.addAttribute("consultationStats", consultationCount);
            model.addAttribute("next7DaysCount", next7DaysCount);
            model.addAttribute("noneCount", 0);
            model.addAttribute("activePatients", activePatients);

            // Appointments for the table
            model.addAttribute("upcomingAppointmentsList", upcomingAppointments);

        } catch (Exception e) {
            // Log error properly
            System.err.println("Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/login?error=dashboardError";
        }

        return "medecin/dashboard";
    }

    @GetMapping("/patient/{id}")
    public String medecinPatientDetails(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpSession session, Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // Get medecin from service
        Medecin medecin = medecinService.getMedecinById(userDetails.getUserId());
        if (medecin == null) {
            return "redirect:/login?error=userNotFound";
        }

        session.setAttribute("user", medecin);

        model.addAttribute("medecin", medecin);
        model.addAttribute("patientId", id);
        return "medecin/patient-details";
    }

    @GetMapping("/consultations")
    public String medecinConsultations(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       HttpSession session, Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // Get medecin from service
        Medecin medecin = medecinService.getMedecinById(userDetails.getUserId());
        if (medecin == null) {
            return "redirect:/login?error=userNotFound";
        }

        session.setAttribute("user", medecin);

        model.addAttribute("medecin", medecin);
        return "medecin/consultations";
    }

    @GetMapping("/consultation/{id}")
    public String medecinConsultationRoom(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                          HttpSession session, Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // Get medecin from service
        Medecin medecin = medecinService.getMedecinById(userDetails.getUserId());
        if (medecin == null) {
            return "redirect:/login?error=userNotFound";
        }

        session.setAttribute("user", medecin);

        model.addAttribute("medecin", medecin);
        model.addAttribute("consultationId", id);
        return "medecin/consultation-room";
    }

    @GetMapping("/notifications")
    public String medecinNotifications(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       HttpSession session, Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        // Get medecin from service
        Medecin medecin = medecinService.getMedecinById(userDetails.getUserId());
        if (medecin == null) {
            return "redirect:/login?error=userNotFound";
        }

        session.setAttribute("user", medecin);

        model.addAttribute("medecin", medecin);
        return "medecin/notifications";
    }

    @GetMapping("/rendezvous/{id}")
    public String viewRendezVousDetails(@PathVariable Long id,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpSession session, Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // Get medecin from service
            Medecin medecin = medecinService.getMedecinById(userDetails.getUserId());
            if (medecin == null) {
                return "redirect:/login?error=userNotFound";
            }

            session.setAttribute("user", medecin);

            RendezVous rendezVous = rendezVousService.getRendezVousById(id);
            if (rendezVous == null || !rendezVous.getMedecin().getId().equals(medecin.getId())) {
                return "redirect:/medecin/dashboard?error=appointmentNotFound";
            }

            model.addAttribute("medecin", medecin);
            model.addAttribute("rendezVous", rendezVous);
            model.addAttribute("patient", rendezVous.getPatient());

            return "medecin/rendezvous-details";

        } catch (Exception e) {
            return "redirect:/medecin/dashboard?error=appointmentError";
        }
    }

    // Helper method to get medecin from userDetails and session
    private Medecin getMedecinFromAuth(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       HttpSession session) {
        if (userDetails == null) {
            return null;
        }

        // Check if medecin is already in session
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin != null && medecin.getId().equals(userDetails.getUserId())) {
            return medecin;
        }

        // If not in session, get from service
        medecin = medecinService.getMedecinById(userDetails.getUserId());
        if (medecin != null) {
            session.setAttribute("user", medecin);
            return medecin;
        }

        return null;
    }
}