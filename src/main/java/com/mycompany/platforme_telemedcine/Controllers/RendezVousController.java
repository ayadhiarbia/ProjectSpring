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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

        // Separate appointments for different UI handling
        List<RendezVous> pendingAppointments = appointments.stream()
                .filter(a -> a.getStatus() == StatusRendezVous.PENDING)
                .collect(Collectors.toList());

        List<RendezVous> approvedAppointments = appointments.stream()
                .filter(a -> a.getStatus() == StatusRendezVous.APPROVED)
                .collect(Collectors.toList());

        List<RendezVous> otherAppointments = appointments.stream()
                .filter(a -> a.getStatus() != StatusRendezVous.PENDING &&
                        a.getStatus() != StatusRendezVous.APPROVED)
                .collect(Collectors.toList());

        model.addAttribute("appointments", appointments);
        model.addAttribute("pendingAppointments", pendingAppointments);
        model.addAttribute("approvedAppointments", approvedAppointments);
        model.addAttribute("otherAppointments", otherAppointments);
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
                    "New appointment requested by " + patient.getName() + " for " + date + " at " + time,
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
            // Check if appointment has a consultation
            if (appointment.getConsultation() != null) {
                // If consultation exists, cancel it first
                Consultation consultation = appointment.getConsultation();
                consultation.cancel("Appointment was cancelled");
                consultationService.updateConsultation(consultation);
            }

            appointment.setStatus(StatusRendezVous.CANCELLED);
            rendezVousService.updateRendezVous(appointment);

            // Notify doctor
            notificationService.createNotification(
                    "Appointment cancelled by " + patient.getName(),
                    appointment.getMedecin().getId()
            );
        }

        return "redirect:/patient/appointments";
    }

    // ============ NEW: CREATE CONSULTATION FROM APPROVED APPOINTMENT ============



    // Submit consultation creation
    @PostMapping("/create-consultation/{id}")
    public String createConsultation(
            @PathVariable Long id,
            @RequestParam ConsultationType consultationType,
            @RequestParam String reason,
            @RequestParam String symptoms,
            HttpSession session,
            Model model) {

        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        RendezVous appointment = rendezVousService.getRendezVousById(id);

        // Validate: appointment exists, belongs to patient, is approved, and has no consultation
        if (appointment == null ||
                !appointment.getPatient().getId().equals(patient.getId()) ||
                appointment.getStatus() != StatusRendezVous.APPROVED ||
                appointment.getConsultation() != null) {

            return "redirect:/patient/appointments?error=invalidAppointment";
        }

        try {
            // Create consultation using the service method
            Consultation consultation = consultationService.createPatientConsultationRequest(
                    patient.getId(),
                    appointment.getMedecin().getId(),
                    consultationType,
                    reason,
                    symptoms,
                    convertToDate(appointment.getDate(), appointment.getTime())
            );

            // Link consultation to appointment
            appointment.setConsultation(consultation);
            rendezVousService.updateRendezVous(appointment);

            // Notify doctor
            notificationService.createNotification(
                    "New consultation request from " + patient.getName() +
                            " for appointment on " + appointment.getDate() + " at " + appointment.getTime(),
                    appointment.getMedecin().getId()
            );

            return "redirect:/patient/consultations?success=consultationCreated";

        } catch (Exception e) {
            System.err.println("ERROR creating consultation: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/patient/appointments?error=consultationFailed";
        }
    }

    // ============ MODIFIED: REMOVE CHOOSE-CONSULTATION (replaced by create-consultation) ============
    // REMOVE THIS METHOD: chooseConsultationType()
    // It's now replaced by createConsultation() above

    // View appointment details
    @GetMapping("/details/{id}")
    public String viewAppointmentDetailsAlt(@PathVariable Long id, HttpSession session, Model model) {
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

        // Add consultation info if exists
        if (appointment.getConsultation() != null) {
            model.addAttribute("consultation", appointment.getConsultation());
        }

        return "patient/appointments-details";
    }

    // Reschedule appointment (only for PENDING appointments)
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
                    "Appointment rescheduled by " + patient.getName() + " for " + newDate + " at " + newTime,
                    appointment.getMedecin().getId()
            );

            return "redirect:/patient/appointments?success=rescheduled";
        }

        return "redirect:/patient/appointments?error=cannotReschedule";
    }

    // ============ HELPER METHODS ============

    private Date convertToDate(LocalDate date, String time) {
        try {
            LocalTime localTime = LocalTime.parse(time);
            LocalDateTime dateTime = LocalDateTime.of(date, localTime);
            return java.sql.Timestamp.valueOf(dateTime);
        } catch (Exception e) {
            // If time parsing fails, use start of day
            return java.sql.Date.valueOf(date);
        }
    }
}