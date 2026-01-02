package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/patient/appointments")
public class RendezVousController {

    private final RendezVousService rendezVousService;
    private final MedecinService medecinService;
    private final NotificationService notificationService;
    private final ConsultationService consultationService;

    @Autowired
    public RendezVousController(
            RendezVousService rendezVousService,
            MedecinService medecinService,
            NotificationService notificationService,
            ConsultationService consultationService) {
        this.rendezVousService = rendezVousService;
        this.medecinService = medecinService;
        this.notificationService = notificationService;
        this.consultationService = consultationService;
    }

    // Get all appointments for patient
    @GetMapping
    public String getPatientAppointments(HttpSession session, Model model, HttpServletResponse response) {
        // Add cache control headers
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        System.out.println("=== DEBUG: Getting appointments for patient ID: " + patient.getId() + " ===");

        List<RendezVous> appointments = new ArrayList<>(); // Start with empty list

        try {
            appointments = rendezVousService.getByPatient(patient.getId());
            System.out.println("SUCCESS: Found " + appointments.size() + " appointments");
        } catch (Exception e) {
            System.err.println("ERROR fetching appointments: " + e.getMessage());
            e.printStackTrace();
            // Keep empty list - don't crash the page
        }

        List<Medecin> doctors = medecinService.getAllMedecin();
        System.out.println("Found " + doctors.size() + " doctors");

        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", doctors);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("patient", patient);

        return "patient/appointments";
    }

    // Book new appointment
    @PostMapping("/book")
    public String bookAppointment(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String time,
            @RequestParam String description,
            HttpSession session) {

        System.out.println("=== BOOKING APPOINTMENT ===");
        System.out.println("Doctor ID: " + doctorId);
        System.out.println("Date: " + date);
        System.out.println("Time: " + time);

        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Medecin doctor = medecinService.getMedecinById(doctorId);
        if (doctor == null) {
            return "redirect:/patient/appointments?error=doctorNotFound";
        }

        RendezVous appointment = new RendezVous();
        appointment.setDate(date);
        appointment.setTime(time);
        appointment.setDescription(description);
        appointment.setPatient(patient);
        appointment.setMedecin(doctor);
        appointment.setStatus(StatusRendezVous.PENDING);

        try {
            rendezVousService.createRendezvous(appointment);
            System.out.println("Appointment saved successfully!");

            // Create notification for doctor
            notificationService.createNotification(
                    "Nouveau rendez-vous demandé par " + patient.getName() + " pour " + date + " à " + time,
                    doctor.getId()
            );

            return "redirect:/patient/appointments?success=true";
        } catch (Exception e) {
            System.err.println("ERROR saving appointment: " + e.getMessage());
            return "redirect:/patient/appointments?error=saveFailed";
        }
    }

    // Cancel appointment
    @PostMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id, HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null && appointment.getPatient().getId().equals(patient.getId())) {
            appointment.setStatus(StatusRendezVous.CANCELLED);
            rendezVousService.updateRendezVous(appointment);

            // Notify doctor
            notificationService.createNotification(
                    "Rendez-vous annulé par " + patient.getName(),
                    appointment.getMedecin().getId()
            );
        }

        return "redirect:/patient/appointments";
    }

    // Choose consultation type
    @PostMapping("/choose-consultation/{id}")
    public String chooseConsultationType(
            @PathVariable Long id,
            @RequestParam String consultationType,
            HttpSession session) {

        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getPatient().getId().equals(patient.getId()) &&
                appointment.getStatus() == StatusRendezVous.APPROVED) {

            // Create consultation
            Consultation consultation = new Consultation();
            consultation.setDate(new Date());

            // Convert String to ConsultationType enum
            ConsultationType type = ConsultationType.valueOf(consultationType.toUpperCase());
            consultation.setConsultationType(type);

            consultation.setCallRoomId("room_" + id + "_" + System.currentTimeMillis());
            consultation.setActive(false);
            consultation.setRendezVous(appointment);

            consultationService.createConsultation(consultation);

            // Update appointment
            appointment.setConsultation(consultation);
            appointment.setStatus(StatusRendezVous.IN_PROGRESS);
            rendezVousService.updateRendezVous(appointment);

            // Notify doctor
            notificationService.createNotification(
                    "Patient a choisi une consultation " + consultationType.toLowerCase(),
                    appointment.getMedecin().getId()
            );

            return "redirect:/patient/consultation/" + consultation.getId();
        }

        return "redirect:/patient/appointments?error=invalidAppointment";
    }

    // Add this method to your controller
    @GetMapping("/details/{id}")
    public String viewAppointmentDetailsAlt(@PathVariable Long id, HttpSession session, Model model) {
        // Same implementation as the other method
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment == null || !appointment.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/appointments?error=notFound";
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);

        return "patient/appointments-details";
    }

    // Reschedule appointment
    @PostMapping("/reschedule/{id}")
    public String rescheduleAppointment(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam String newTime,
            HttpSession session) {

        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getPatient().getId().equals(patient.getId()) &&
                appointment.getStatus() == StatusRendezVous.PENDING) {

            appointment.setDate(newDate);
            appointment.setTime(newTime);
            appointment.setStatus(StatusRendezVous.PENDING); // Reset to pending for doctor approval
            rendezVousService.updateRendezVous(appointment);

            // Notify doctor
            notificationService.createNotification(
                    "Rendez-vous reprogrammé par " + patient.getName() + " pour " + newDate + " à " + newTime,
                    appointment.getMedecin().getId()
            );

            return "redirect:/patient/appointments?success=rescheduled";
        }

        return "redirect:/patient/appointments?error=cannotReschedule";
    }
}