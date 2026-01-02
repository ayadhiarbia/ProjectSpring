package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final PatientService patientService;
    private final MedecinService medecinService;
    private final RendezVousService rendezVousService;
    private final ConsultationService consultationService;
    private final OrdonanceService ordonanceService;

    @Autowired
    public NotificationController(SimpMessagingTemplate messagingTemplate,
                                  NotificationService notificationService,
                                  PatientService patientService,
                                  MedecinService medecinService,
                                  RendezVousService rendezVousService,
                                  ConsultationService consultationService,
                                  OrdonanceService ordonanceService) {
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.patientService = patientService;
        this.medecinService = medecinService;
        this.rendezVousService = rendezVousService;
        this.consultationService = consultationService;
        this.ordonanceService = ordonanceService;
    }

    // ==================== WEB SOCKET NOTIFICATIONS ====================

    /**
     * Send real-time notification to doctor via WebSocket
     */
    private void sendWebSocketNotification(Long medecinId, String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("medecinId", medecinId);

        // Send to doctor-specific channel
        messagingTemplate.convertAndSend("/topic/notifications/doctor/" + medecinId, notification);

        // Also send to general doctor channel
        messagingTemplate.convertAndSend("/topic/notifications/doctors", notification);
    }

    /**
     * Send real-time notification to patient via WebSocket
     */
    private void sendPatientWebSocketNotification(Long patientId, String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now().toString());
        notification.put("patientId", patientId);

        // Send to patient-specific channel
        messagingTemplate.convertAndSend("/topic/notifications/patient/" + patientId, notification);
    }

    // ==================== APPOINTMENT NOTIFICATIONS ====================

    /**
     * Send appointment reminder notifications
     * This should be scheduled to run daily
     */
    @PostMapping("/appointments/reminders")
    public ResponseEntity<Map<String, Object>> sendAppointmentReminders() {
        Map<String, Object> response = new HashMap<>();
        int sentCount = 0;

        // Get today's appointments
        List<RendezVous> todayAppointments = rendezVousService.getAllRendezVous().stream()
                .filter(rv -> rv.getDate() != null && rv.getDate().equals(LocalDate.now()))
                .collect(Collectors.toList());

        // Get tomorrow's appointments
        List<RendezVous> tomorrowAppointments = rendezVousService.getAllRendezVous().stream()
                .filter(rv -> rv.getDate() != null && rv.getDate().equals(LocalDate.now().plusDays(1)))
                .collect(Collectors.toList());

        // Send reminders for today's appointments
        for (RendezVous rv : todayAppointments) {
            if (rv.getMedecin() != null && rv.getPatient() != null) {
                // Notify doctor
                String doctorMessage = "Rendez-vous aujourd'hui à " + rv.getTime() +
                        " avec " + rv.getPatient().getName();
                notificationService.createNotification(doctorMessage, rv.getMedecin().getId());
                sendWebSocketNotification(rv.getMedecin().getId(), doctorMessage, "APPOINTMENT_REMINDER");

                // Notify patient
                String patientMessage = "Votre rendez-vous avec Dr. " + rv.getMedecin().getName() +
                        " est aujourd'hui à " + rv.getTime();
                sendPatientWebSocketNotification(rv.getPatient().getId(), patientMessage, "APPOINTMENT_REMINDER");

                sentCount++;
            }
        }

        // Send reminders for tomorrow's appointments
        for (RendezVous rv : tomorrowAppointments) {
            if (rv.getMedecin() != null && rv.getPatient() != null) {
                // Notify doctor
                String doctorMessage = "Rendez-vous demain à " + rv.getTime() +
                        " avec " + rv.getPatient().getName();
                notificationService.createNotification(doctorMessage, rv.getMedecin().getId());

                // Notify patient
                String patientMessage = "Rappel: Vous avez un rendez-vous demain avec Dr. " +
                        rv.getMedecin().getName() + " à " + rv.getTime();
                sendPatientWebSocketNotification(rv.getPatient().getId(), patientMessage, "APPOINTMENT_REMINDER");

                sentCount++;
            }
        }

        response.put("success", true);
        response.put("remindersSent", sentCount);
        response.put("todayAppointments", todayAppointments.size());
        response.put("tomorrowAppointments", tomorrowAppointments.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Notify about new appointment request
     */
    @PostMapping("/appointments/new/{appointmentId}")
    public ResponseEntity<Map<String, Object>> notifyNewAppointment(@PathVariable Long appointmentId) {
        Map<String, Object> response = new HashMap<>();

        RendezVous appointment = rendezVousService.getRendezVousById(appointmentId);
        if (appointment == null || appointment.getMedecin() == null) {
            response.put("success", false);
            response.put("message", "Appointment not found or invalid");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String message = "Nouvelle demande de rendez-vous de " +
                appointment.getPatient().getName() +
                " pour le " + appointment.getDate() + " à " + appointment.getTime();

        // Create database notification
        Notification notification = notificationService.createNotification(message, appointment.getMedecin().getId());

        // Send WebSocket notification
        sendWebSocketNotification(appointment.getMedecin().getId(), message, "NEW_APPOINTMENT");

        response.put("success", true);
        response.put("notificationId", notification.getId());
        response.put("message", "Notification sent successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Notify about appointment status change
     */
    @PostMapping("/appointments/status/{appointmentId}")
    public ResponseEntity<Map<String, Object>> notifyAppointmentStatus(@PathVariable Long appointmentId) {
        Map<String, Object> response = new HashMap<>();

        RendezVous appointment = rendezVousService.getRendezVousById(appointmentId);
        if (appointment == null || appointment.getMedecin() == null || appointment.getPatient() == null) {
            response.put("success", false);
            response.put("message", "Appointment not found or invalid");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Notify doctor
        String doctorMessage = "Statut du rendez-vous avec " + appointment.getPatient().getName() +
                " mis à jour: " + appointment.getStatus();
        notificationService.createNotification(doctorMessage, appointment.getMedecin().getId());
        sendWebSocketNotification(appointment.getMedecin().getId(), doctorMessage, "APPOINTMENT_STATUS");

        // Notify patient
        String patientMessage = "Votre rendez-vous avec Dr. " + appointment.getMedecin().getName() +
                " est maintenant: " + appointment.getStatus();
        sendPatientWebSocketNotification(appointment.getPatient().getId(), patientMessage, "APPOINTMENT_STATUS");

        response.put("success", true);
        response.put("message", "Status notifications sent successfully");

        return ResponseEntity.ok(response);
    }

    // ==================== MESSAGE NOTIFICATIONS ====================

    /**
     * Notify about new message
     */
    @PostMapping("/messages/new")
    public ResponseEntity<Map<String, Object>> notifyNewMessage(
            @RequestParam Long senderId,
            @RequestParam Long recipientId,
            @RequestParam String messageContent,
            @RequestParam String senderType) { // "PATIENT" or "MEDECIN"

        Map<String, Object> response = new HashMap<>();
        String message;

        if ("PATIENT".equalsIgnoreCase(senderType)) {
            // Patient sending to doctor
            Patient patient = patientService.getPatientById(senderId);
            Medecin doctor = medecinService.getMedecinById(recipientId);

            if (patient == null || doctor == null) {
                response.put("success", false);
                response.put("message", "Invalid sender or recipient");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            message = "Nouveau message de " + patient.getName() + ": " +
                    (messageContent.length() > 50 ? messageContent.substring(0, 50) + "..." : messageContent);

            // Create database notification for doctor
            notificationService.createNotification(message, doctor.getId());

            // Send WebSocket notification
            sendWebSocketNotification(doctor.getId(), message, "NEW_MESSAGE");

        } else if ("MEDECIN".equalsIgnoreCase(senderType)) {
            // Doctor sending to patient
            Medecin doctor = medecinService.getMedecinById(senderId);
            Patient patient = patientService.getPatientById(recipientId);

            if (doctor == null || patient == null) {
                response.put("success", false);
                response.put("message", "Invalid sender or recipient");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            message = "Nouveau message de Dr. " + doctor.getName() + ": " +
                    (messageContent.length() > 50 ? messageContent.substring(0, 50) + "..." : messageContent);

            // Send WebSocket notification to patient
            sendPatientWebSocketNotification(patient.getId(), message, "NEW_MESSAGE");
        } else {
            response.put("success", false);
            response.put("message", "Invalid sender type");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        response.put("success", true);
        response.put("message", "Message notification sent successfully");

        return ResponseEntity.ok(response);
    }

    // ==================== TEST RESULTS NOTIFICATIONS ====================

    /**
     * Notify about new test results
     */
    @PostMapping("/test-results/new/{patientId}")
    public ResponseEntity<Map<String, Object>> notifyNewTestResult(
            @PathVariable Long patientId,
            @RequestParam String testType,
            @RequestParam Long doctorId) {

        Map<String, Object> response = new HashMap<>();

        Patient patient = patientService.getPatientById(patientId);
        Medecin doctor = medecinService.getMedecinById(doctorId);

        if (patient == null || doctor == null) {
            response.put("success", false);
            response.put("message", "Invalid patient or doctor");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Notify doctor
        String doctorMessage = "Nouveaux résultats de test pour " + patient.getName() +
                " (" + testType + ")";
        Notification notification = notificationService.createNotification(doctorMessage, doctor.getId());
        sendWebSocketNotification(doctor.getId(), doctorMessage, "NEW_TEST_RESULT");

        // Notify patient
        String patientMessage = "Vos résultats de test " + testType + " sont disponibles";
        sendPatientWebSocketNotification(patient.getId(), patientMessage, "TEST_RESULT_READY");

        response.put("success", true);
        response.put("notificationId", notification.getId());
        response.put("message", "Test result notifications sent successfully");

        return ResponseEntity.ok(response);
    }

    // ==================== PRESCRIPTION NOTIFICATIONS ====================

    /**
     * Notify about new prescription
     */
    @PostMapping("/prescriptions/new/{prescriptionId}")
    public ResponseEntity<Map<String, Object>> notifyNewPrescription(@PathVariable Long prescriptionId) {
        Map<String, Object> response = new HashMap<>();

        Ordonance prescription = ordonanceService.findOrdonanceById(prescriptionId);
        if (prescription == null || prescription.getConsultation() == null ||
                prescription.getConsultation().getRendezVous() == null) {
            response.put("success", false);
            response.put("message", "Prescription not found or invalid");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        RendezVous appointment = prescription.getConsultation().getRendezVous();
        Patient patient = appointment.getPatient();
        Medecin doctor = appointment.getMedecin();

        // Notify doctor (optional - usually doctor creates it)
        String doctorMessage = "Ordonnance créée pour " + patient.getName();
        notificationService.createNotification(doctorMessage, doctor.getId());
        sendWebSocketNotification(doctor.getId(), doctorMessage, "PRESCRIPTION_CREATED");

        // Notify patient
        String patientMessage = "Une nouvelle ordonnance vous a été prescrite par Dr. " +
                doctor.getName() + ". Veuillez consulter vos documents médicaux.";
        sendPatientWebSocketNotification(patient.getId(), patientMessage, "NEW_PRESCRIPTION");

        response.put("success", true);
        response.put("message", "Prescription notifications sent successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Notify about prescription renewal reminder
     */
    @PostMapping("/prescriptions/renewal-reminder/{patientId}")
    public ResponseEntity<Map<String, Object>> notifyPrescriptionRenewal(
            @PathVariable Long patientId,
            @RequestParam String medicationName,
            @RequestParam Long doctorId) {

        Map<String, Object> response = new HashMap<>();

        Patient patient = patientService.getPatientById(patientId);
        Medecin doctor = medecinService.getMedecinById(doctorId);

        if (patient == null || doctor == null) {
            response.put("success", false);
            response.put("message", "Invalid patient or doctor");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // Notify doctor
        String doctorMessage = patient.getName() + " a besoin d'un renouvellement pour: " + medicationName;
        notificationService.createNotification(doctorMessage, doctor.getId());
        sendWebSocketNotification(doctor.getId(), doctorMessage, "PRESCRIPTION_RENEWAL");

        response.put("success", true);
        response.put("message", "Prescription renewal reminder sent");

        return ResponseEntity.ok(response);
    }

    // ==================== CONSULTATION NOTIFICATIONS ====================

    /**
     * Notify about upcoming video consultation
     */
    @PostMapping("/consultations/video-reminder/{consultationId}")
    public ResponseEntity<Map<String, Object>> notifyVideoConsultation(@PathVariable Long consultationId) {
        Map<String, Object> response = new HashMap<>();

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || consultation.getRendezVous() == null) {
            response.put("success", false);
            response.put("message", "Consultation not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        RendezVous appointment = consultation.getRendezVous();
        Patient patient = appointment.getPatient();
        Medecin doctor = appointment.getMedecin();

        // Notify doctor 15 minutes before
        String doctorMessage = "Consultation vidéo dans 15 minutes avec " + patient.getName() +
                ". Room ID: " + consultation.getCallRoomId();
        notificationService.createNotification(doctorMessage, doctor.getId());
        sendWebSocketNotification(doctor.getId(), doctorMessage, "VIDEO_CONSULTATION_REMINDER");

        // Notify patient
        String patientMessage = "Votre consultation vidéo avec Dr. " + doctor.getName() +
                " commence dans 15 minutes. Room ID: " + consultation.getCallRoomId();
        sendPatientWebSocketNotification(patient.getId(), patientMessage, "VIDEO_CONSULTATION_REMINDER");

        response.put("success", true);
        response.put("message", "Video consultation reminders sent");

        return ResponseEntity.ok(response);
    }

    /**
     * Notify when patient joins consultation room
     */
    @PostMapping("/consultations/patient-joined/{consultationId}")
    public ResponseEntity<Map<String, Object>> notifyPatientJoinedConsultation(@PathVariable Long consultationId) {
        Map<String, Object> response = new HashMap<>();

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || consultation.getRendezVous() == null) {
            response.put("success", false);
            response.put("message", "Consultation not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Medecin doctor = consultation.getRendezVous().getMedecin();

        String message = "Le patient a rejoint la salle de consultation vidéo";
        notificationService.createNotification(message, doctor.getId());
        sendWebSocketNotification(doctor.getId(), message, "PATIENT_JOINED_CONSULTATION");

        response.put("success", true);
        response.put("message", "Doctor notified");

        return ResponseEntity.ok(response);
    }

    // ==================== SYSTEM ALERTS ====================

    /**
     * Send system-wide alert
     */
    @PostMapping("/system-alerts")
    public ResponseEntity<Map<String, Object>> sendSystemAlert(
            @RequestParam String alertMessage,
            @RequestParam String alertLevel) { // INFO, WARNING, CRITICAL

        Map<String, Object> response = new HashMap<>();

        // Send to all doctors
        List<Medecin> allDoctors = medecinService.getAllMedecins();
        for (Medecin doctor : allDoctors) {
            String message = "Alerte système [" + alertLevel + "]: " + alertMessage;
            notificationService.createNotification(message, doctor.getId());
            sendWebSocketNotification(doctor.getId(), message, "SYSTEM_ALERT");
        }

        // Broadcast to general system channel
        Map<String, Object> systemAlert = new HashMap<>();
        systemAlert.put("type", "SYSTEM_ALERT");
        systemAlert.put("level", alertLevel);
        systemAlert.put("message", alertMessage);
        systemAlert.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/notifications/system", systemAlert);

        response.put("success", true);
        response.put("doctorsNotified", allDoctors.size());
        response.put("message", "System alert sent successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Notify about system maintenance
     */
    @PostMapping("/system-maintenance")
    public ResponseEntity<Map<String, Object>> notifySystemMaintenance(
            @RequestParam String maintenanceMessage,
            @RequestParam String scheduledTime) {

        Map<String, Object> response = new HashMap<>();

        // Send to all doctors
        List<Medecin> allDoctors = medecinService.getAllMedecins();
        for (Medecin doctor : allDoctors) {
            String message = "Maintenance système prévue à " + scheduledTime + ": " + maintenanceMessage;
            notificationService.createNotification(message, doctor.getId());
            sendWebSocketNotification(doctor.getId(), message, "SYSTEM_MAINTENANCE");
        }

        // Send to all patients (optional - through WebSocket)
        // In a real system, you'd have a list of active patients

        response.put("success", true);
        response.put("doctorsNotified", allDoctors.size());
        response.put("message", "Maintenance notification sent");

        return ResponseEntity.ok(response);
    }

    // ==================== NOTIFICATION MANAGEMENT ====================

    /**
     * Get notifications for a doctor
     */
    @GetMapping("/doctor/{medecinId}")
    public ResponseEntity<List<Notification>> getDoctorNotifications(@PathVariable Long medecinId) {
        List<Notification> notifications = notificationService.getNotificationsByMedecinId(medecinId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for a doctor
     */
    @GetMapping("/doctor/{medecinId}/unread")
    public ResponseEntity<List<Notification>> getUnreadDoctorNotifications(@PathVariable Long medecinId) {
        List<Notification> notifications = notificationService.getUnreadNotificationsByMedecinId(medecinId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Notification> markNotificationAsRead(@PathVariable Long notificationId) {
        Notification notification = notificationService.markAsRead(notificationId);
        if (notification == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(notification);
    }

    /**
     * Mark all notifications as read for a doctor
     */
    @PutMapping("/doctor/{medecinId}/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@PathVariable Long medecinId) {
        Map<String, Object> response = new HashMap<>();

        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByMedecinId(medecinId);
        int markedCount = 0;

        for (Notification notification : unreadNotifications) {
            notificationService.markAsRead(notification.getId());
            markedCount++;
        }

        response.put("success", true);
        response.put("markedAsRead", markedCount);
        response.put("message", "All notifications marked as read");

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable Long notificationId) {
        Map<String, Object> response = new HashMap<>();

        notificationService.deleteNotification(notificationId);

        response.put("success", true);
        response.put("message", "Notification deleted successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Clear all notifications for a doctor
     */
    @DeleteMapping("/doctor/{medecinId}/clear")
    public ResponseEntity<Map<String, Object>> clearAllNotifications(@PathVariable Long medecinId) {
        Map<String, Object> response = new HashMap<>();

        notificationService.deleteAllNotificationsByMedecinId(medecinId);

        response.put("success", true);
        response.put("message", "All notifications cleared");

        return ResponseEntity.ok(response);
    }

    /**
     * Get notification statistics
     */
    @GetMapping("/doctor/{medecinId}/stats")
    public ResponseEntity<Map<String, Object>> getNotificationStats(@PathVariable Long medecinId) {
        Map<String, Object> stats = new HashMap<>();

        List<Notification> allNotifications = notificationService.getNotificationsByMedecinId(medecinId);
        List<Notification> unreadNotifications = notificationService.getUnreadNotificationsByMedecinId(medecinId);

        long todayCount = allNotifications.stream()
                .filter(n -> n.getCreatedAt() != null &&
                        n.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .count();

        stats.put("totalNotifications", allNotifications.size());
        stats.put("unreadNotifications", unreadNotifications.size());
        stats.put("todayNotifications", todayCount);
        stats.put("readNotifications", allNotifications.size() - unreadNotifications.size());

        return ResponseEntity.ok(stats);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Schedule appointment reminders (to be called by a scheduled task)
     */
    public void scheduleDailyReminders() {
        // This method should be called by @Scheduled annotation
        sendAppointmentReminders();
    }

    /**
     * Check for upcoming consultations (to be called by a scheduled task)
     */
    public void checkUpcomingConsultations() {
        // Check for consultations starting in next 15 minutes
        List<Consultation> upcomingConsultations = consultationService.getAllConsultations().stream()
                .filter(c -> c.getDate() != null && !c.getActive())
                .filter(c -> {
                    LocalDateTime consultationTime = c.getDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime();
                    LocalDateTime now = LocalDateTime.now();
                    return consultationTime.isAfter(now) &&
                            consultationTime.isBefore(now.plusMinutes(16));
                })
                .collect(Collectors.toList());

        for (Consultation consultation : upcomingConsultations) {
            notifyVideoConsultation(consultation.getId());
        }
    }

    public void sendNotificationToMedecin(Long medecinId, String message) {

    }
}