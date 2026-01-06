package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/medecin/rendezvous")
public class DoctorAppointmentsController {

    private final RendezVousService rendezVousService;
    private final PatientService patientService;
    private final NotificationService notificationService;
    private final ConsultationService consultationService;

    @Autowired
    public DoctorAppointmentsController(
            RendezVousService rendezVousService,
            PatientService patientService,
            NotificationService notificationService,
            ConsultationService consultationService) {
        this.rendezVousService = rendezVousService;
        this.patientService = patientService;
        this.notificationService = notificationService;
        this.consultationService = consultationService;
    }

    // GET: Doctor's appointments dashboard
    @GetMapping
    public String getDoctorAppointments(HttpSession session, Model model) {
        Object user = session.getAttribute("user");

        // Check if user is a doctor
        if (!(user instanceof Medecin)) {
            // Check if user is a patient (common mix-up)
            if (user instanceof Patient) {
                // Redirect patient to their own appointments page
                return "redirect:/patient/rendezvous?error=wrong-access";
            }
            // Not logged in or wrong user type
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        List<RendezVous> allAppointments = getRendezVousByDoctor(doctor.getId());

        // Group appointments by status
        Map<StatusRendezVous, List<RendezVous>> appointmentsByStatus = allAppointments.stream()
                .collect(Collectors.groupingBy(RendezVous::getStatus));

        // Get today's appointments - FIXED with null check
        List<RendezVous> todayAppointments = allAppointments.stream()
                .filter(rv -> rv.getDate() != null) // Check for null date first
                .filter(rv -> rv.getDate().equals(LocalDate.now()))
                .sorted(Comparator.comparing(RendezVous::getTime))
                .collect(Collectors.toList());

        // Get upcoming appointments (next 7 days) - FIXED with null checks
        List<RendezVous> upcomingAppointments = allAppointments.stream()
                .filter(rv -> rv.getDate() != null) // Check for null date first
                .filter(rv -> rv.getDate().isAfter(LocalDate.now()))
                .filter(rv -> rv.getDate().isBefore(LocalDate.now().plusDays(8)))
                .sorted(Comparator.comparing(RendezVous::getDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RendezVous::getTime,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Get pending approval appointments
        List<RendezVous> pendingApproval = appointmentsByStatus.getOrDefault(StatusRendezVous.PENDING, new ArrayList<>())
                .stream()
                .filter(rv -> rv.getDate() != null) // Add null check here too
                .sorted(Comparator.comparing(RendezVous::getDate))
                .collect(Collectors.toList());

        // Statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAppointments", allAppointments.size());
        stats.put("todayAppointments", todayAppointments.size());
        stats.put("pendingApproval", pendingApproval.size());
        stats.put("upcomingAppointments", upcomingAppointments.size());
        stats.put("completedAppointments", appointmentsByStatus.getOrDefault(StatusRendezVous.COMPLETED, new ArrayList<>()).size());

        // Add helper method for status badge classes
        model.addAttribute("getStatusBadgeClass", (Function<StatusRendezVous, String>) this::getStatusBadgeClass);

        model.addAttribute("doctor", doctor);
        model.addAttribute("allAppointments", allAppointments);
        model.addAttribute("appointmentsByStatus", appointmentsByStatus);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        model.addAttribute("pendingApproval", pendingApproval);
        model.addAttribute("stats", stats);
        model.addAttribute("today", LocalDate.now());

        return "medecin/rendezvous";
    }

    // Helper method to get CSS classes for status badges
    private String getStatusBadgeClass(StatusRendezVous status) {
        if (status == null) {
            return "bg-slate-50 text-slate-600";
        }

        switch (status) {
            case APPROVED:
                return "bg-green-50 text-green-600";
            case PENDING:
                return "bg-amber-50 text-amber-600";
            case IN_PROGRESS:
                return "bg-blue-50 text-blue-600";
            case COMPLETED:
                return "bg-purple-50 text-purple-600";
            case REJECTED:
                return "bg-red-50 text-red-600";
            case CANCELLED:
                return "bg-slate-50 text-slate-600";
            default:
                return "bg-slate-50 text-slate-600";
        }
    }

    // POST: Approve appointment
    @PostMapping("/approve/{id}")
    public String approveAppointment(@PathVariable Long id,
                                     @RequestParam(required = false) String notes,
                                     HttpSession session) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getMedecin().getId().equals(doctor.getId()) &&
                appointment.getStatus() == StatusRendezVous.PENDING) {

            appointment.setStatus(StatusRendezVous.APPROVED);
            if (notes != null && !notes.trim().isEmpty()) {
                appointment.setDescription(appointment.getDescription() +
                        "\n\n[Note du médecin]: " + notes);
            }
            rendezVousService.updateRendezVous(appointment);

            // Notify patient
            notificationService.createNotification(
                    "Votre rendez-vous avec Dr. " + doctor.getName() +
                            " le " + appointment.getDate() + " à " + appointment.getTime() +
                            " a été approuvé.",
                    appointment.getPatient().getId()
            );

            return "redirect:/medecin/rendezvous?success=approved&id=" + id;
        }

        return "redirect:/medecin/rendezvous?error=cannotApprove";
    }

    // POST: Reject appointment
    @PostMapping("/reject/{id}")
    public String rejectAppointment(@PathVariable Long id,
                                    @RequestParam String reason,
                                    HttpSession session) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getMedecin().getId().equals(doctor.getId()) &&
                appointment.getStatus() == StatusRendezVous.PENDING) {

            appointment.setStatus(StatusRendezVous.REJECTED);
            appointment.setDescription(appointment.getDescription() +
                    "\n\n[Raison du rejet]: " + reason);
            rendezVousService.updateRendezVous(appointment);

            // Notify patient
            notificationService.createNotification(
                    "Votre rendez-vous avec Dr. " + doctor.getName() +
                            " a été rejeté. Raison: " + reason,
                    appointment.getPatient().getId()
            );

            return "redirect:/medecin/rendezvous?success=rejected&id=" + id;
        }

        return "redirect:/medecin/rendezvous?error=cannotReject";
    }

    // POST: Mark appointment as completed
    @PostMapping("/complete/{id}")
    public String completeAppointment(@PathVariable Long id, HttpSession session) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getMedecin().getId().equals(doctor.getId()) &&
                (appointment.getStatus() == StatusRendezVous.APPROVED ||
                        appointment.getStatus() == StatusRendezVous.IN_PROGRESS)) {

            appointment.setStatus(StatusRendezVous.COMPLETED);
            rendezVousService.updateRendezVous(appointment);

            // Notify patient
            notificationService.createNotification(
                    "Votre rendez-vous avec Dr. " + doctor.getName() +
                            " est marqué comme terminé.",
                    appointment.getPatient().getId()
            );

            return "redirect:/medecin/rendezvous?success=completed&id=" + id;
        }

        return "redirect:/medecin/rendezvous?error=cannotComplete";
    }

    // GET: View appointment details
    @GetMapping("/details/{id}")
    public String viewAppointmentDetails(@PathVariable Long id, HttpSession session, Model model) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment == null || !appointment.getMedecin().getId().equals(doctor.getId())) {
            return "redirect:/medecin/rendezvous?error=notFound";
        }

        Consultation consultation = appointment.getConsultation();
        Patient patient = appointment.getPatient();

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", patient);
        model.addAttribute("consultation", consultation);
        model.addAttribute("canStartConsultation",
                appointment.getStatus() == StatusRendezVous.APPROVED &&
                        consultation == null);

        return "medecin/patient-details";
    }

    // GET: Show consultation start page (select consultation type)
    @GetMapping("/consultation/start/{id}")
    public String showConsultationStartPage(@PathVariable Long id, HttpSession session, Model model) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment == null || !appointment.getMedecin().getId().equals(doctor.getId())) {
            return "redirect:/medecin/rendezvous?error=notFound";
        }

        // Check if consultation can be started
        if (appointment.getStatus() != StatusRendezVous.APPROVED || appointment.getConsultation() != null) {
            return "redirect:/medecin/rendezvous/details/" + id + "?error=cannotStartConsultation";
        }

        model.addAttribute("appointment", appointment);
        model.addAttribute("patient", appointment.getPatient());
        model.addAttribute("consultationTypes", ConsultationType.values());

        return "medecin/start-consultation";
    }

    // POST: Start consultation
    @PostMapping("/start-consultation/{id}")
    public String startConsultation(@PathVariable Long id,
                                    @RequestParam String consultationType,
                                    HttpSession session) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getMedecin().getId().equals(doctor.getId()) &&
                appointment.getStatus() == StatusRendezVous.APPROVED &&
                appointment.getConsultation() == null) {

            try {
                // Create consultation using your model constructor
                Consultation consultation = new Consultation(appointment,
                        ConsultationType.valueOf(consultationType.toUpperCase()));

                // Set consultation as active (your model uses setIsActive)
                consultation.setIsActive(true);
                consultation.setDate(new Date());
                consultation.setCallRoomId("room_" + id + "_" + System.currentTimeMillis());

                consultationService.createConsultation(consultation);

                // Update appointment
                appointment.setConsultation(consultation);
                appointment.setStatus(StatusRendezVous.IN_PROGRESS);
                rendezVousService.updateRendezVous(appointment);

                // Notify patient
                notificationService.createNotification(
                        "Le Dr. " + doctor.getName() + " a démarré la consultation. Rejoignez la salle de consultation.",
                        appointment.getPatient().getId()
                );

                return "redirect:/medecin/rendezvous/consultation/room/" + consultation.getId();

            } catch (IllegalArgumentException e) {
                // Invalid consultation type
                return "redirect:/medecin/rendezvous/details/" + id + "?error=invalidConsultationType";
            }
        }

        return "redirect:/medecin/rendezvous/details/" + id + "?error=cannotStart";
    }

    // GET: Consultation room
    @GetMapping("/consultation/room/{id}")
    public String consultationRoom(@PathVariable Long id, HttpSession session, Model model) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        Consultation consultation = consultationService.getConsultationById(id);
        if (consultation == null) {
            return "redirect:/medecin/rendezvous?error=consultationNotFound";
        }

        // Verify the consultation belongs to this doctor
        if (consultation.getRendezVous() == null ||
                !consultation.getRendezVous().getMedecin().getId().equals(doctor.getId())) {
            return "redirect:/medecin/rendezvous?error=accessDenied";
        }

        model.addAttribute("consultation", consultation);
        model.addAttribute("patient", consultation.getRendezVous().getPatient());
        model.addAttribute("doctor", doctor);
        model.addAttribute("roomId", consultation.getCallRoomId());

        return "medecin/consultation-room";
    }

    // POST: End consultation
    @PostMapping("/consultation/end/{id}")
    public String endConsultation(@PathVariable Long id,
                                  @RequestParam(required = false) String notes,
                                  HttpSession session) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        Consultation consultation = consultationService.getConsultationById(id);
        if (consultation != null &&
                consultation.getRendezVous() != null &&
                consultation.getRendezVous().getMedecin().getId().equals(doctor.getId())) {

            // End consultation
            consultation.setIsActive(false);
            if (notes != null && !notes.trim().isEmpty()) {
                consultation.setNotes(consultation.getNotes() != null ?
                        consultation.getNotes() + "\n\n" + notes : notes);
            }
            consultationService.updateConsultation(consultation);

            // Update appointment status
            RendezVous appointment = consultation.getRendezVous();
            appointment.setStatus(StatusRendezVous.COMPLETED);
            rendezVousService.updateRendezVous(appointment);

            // Notify patient
            notificationService.createNotification(
                    "La consultation avec Dr. " + doctor.getName() + " est terminée.",
                    appointment.getPatient().getId()
            );

            return "redirect:/medecin/rendezvous/details/" + appointment.getId() + "?success=consultationEnded";
        }

        return "redirect:/medecin/rendezvous?error=cannotEndConsultation";
    }

    // POST: Reschedule appointment (doctor-initiated)
    @PostMapping("/reschedule/{id}")
    public String doctorRescheduleAppointment(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newDate,
            @RequestParam String newTime,
            @RequestParam String reason,
            HttpSession session) {

        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getMedecin().getId().equals(doctor.getId()) &&
                appointment.getStatus() != StatusRendezVous.COMPLETED &&
                appointment.getStatus() != StatusRendezVous.CANCELLED) {

            // Save old appointment details
            LocalDate oldDate = appointment.getDate();
            String oldTime = appointment.getTime();

            // Update appointment
            appointment.setDate(newDate);
            appointment.setTime(newTime);
            appointment.setStatus(StatusRendezVous.PENDING); // Needs patient confirmation
            appointment.setDescription(appointment.getDescription() +
                    "\n\n[Reprogrammation par le médecin]: " + reason +
                    "\nAncienne date: " + oldDate + " à " + oldTime +
                    "\nNouvelle date: " + newDate + " à " + newTime);

            rendezVousService.updateRendezVous(appointment);

            // Notify patient
            notificationService.createNotification(
                    "Le Dr. " + doctor.getName() + " a proposé une nouvelle date pour votre rendez-vous: " +
                            newDate + " à " + newTime + ". Raison: " + reason,
                    appointment.getPatient().getId()
            );

            return "redirect:/medecin/rendezvous/details/" + id + "?success=rescheduled";
        }

        return "redirect:/medecin/rendezvous?error=cannotReschedule";
    }

    // POST: Cancel appointment
    @PostMapping("/cancel/{id}")
    public String cancelAppointment(@PathVariable Long id,
                                    @RequestParam String reason,
                                    HttpSession session) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        RendezVous appointment = rendezVousService.getRendezVousById(id);
        if (appointment != null &&
                appointment.getMedecin().getId().equals(doctor.getId()) &&
                appointment.getStatus() != StatusRendezVous.COMPLETED &&
                appointment.getStatus() != StatusRendezVous.CANCELLED) {

            appointment.setStatus(StatusRendezVous.CANCELLED);
            appointment.setDescription(appointment.getDescription() +
                    "\n\n[Annulation par le médecin]: " + reason);
            rendezVousService.updateRendezVous(appointment);

            // Notify patient
            notificationService.createNotification(
                    "Votre rendez-vous avec Dr. " + doctor.getName() +
                            " a été annulé. Raison: " + reason,
                    appointment.getPatient().getId()
            );

            return "redirect:/medecin/rendezvous?success=cancelled&id=" + id;
        }

        return "redirect:/medecin/rendezvous?error=cannotCancel";
    }

    // Helper method to get appointments for doctor (with safe casting)
    private List<RendezVous> getRendezVousByDoctor(Long doctorId) {
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        return objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .filter(rv -> rv.getDate() != null) // Filter out null dates early
                .collect(Collectors.toList());
    }

    // Helper: Get consultation by appointment ID
    private Consultation getConsultationByAppointmentId(Long appointmentId) {
        // Get all consultations and filter
        List<Consultation> allConsultations = consultationService.getAllConsultations();
        return allConsultations.stream()
                .filter(c -> c.getRendezVous() != null && c.getRendezVous().getId().equals(appointmentId))
                .findFirst()
                .orElse(null);
    }
}