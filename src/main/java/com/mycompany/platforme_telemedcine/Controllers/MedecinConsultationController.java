package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import com.mycompany.platforme_telemedcine.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/medecin/consultation")
public class MedecinConsultationController {

    private final ConsultationService consultationService;
    private final RendezVousService rendezVousService;
    private final MedecinService medecinService;
    private final PatientService patientService;
    private final NotificationService notificationService;
    private final OrdonanceService ordonanceService;
    private final DossierMedicalService dossierMedicalService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public MedecinConsultationController(
            ConsultationService consultationService,
            RendezVousService rendezVousService,
            MedecinService medecinService,
            PatientService patientService,
            NotificationService notificationService,
            OrdonanceService ordonanceService,
            DossierMedicalService dossierMedicalService,
            SimpMessagingTemplate messagingTemplate) {
        this.consultationService = consultationService;
        this.rendezVousService = rendezVousService;
        this.medecinService = medecinService;
        this.patientService = patientService;
        this.notificationService = notificationService;
        this.ordonanceService = ordonanceService;
        this.dossierMedicalService = dossierMedicalService;
        this.messagingTemplate = messagingTemplate;
    }

    // GET: Doctor consultation dashboard
    @GetMapping("/dashboard")
    public String consultationDashboard(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        // Get today's consultations
        List<Consultation> todayConsultations = consultationService.getTodayConsultationsForMedecin(medecin.getId());

        // Get waiting consultations
        List<Consultation> waitingConsultations = consultationService.getWaitingConsultationsForMedecin(medecin.getId());

        // Get active consultations
        List<Consultation> activeConsultations = consultationService.getActiveConsultationsForMedecin(medecin.getId());

        model.addAttribute("medecin", medecin);
        model.addAttribute("todayConsultations", todayConsultations);
        model.addAttribute("waitingConsultations", waitingConsultations);
        model.addAttribute("activeConsultations", activeConsultations);

        return "medecin/consultations";
    }

    // GET: Doctor consultation room
    @GetMapping("/room/{consultationId}")
    public String consultationRoom(@PathVariable Long consultationId, HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
            return "redirect:/medecin/consultations?error=invalidConsultation";
        }

        Patient patient = consultation.getRendezVous().getPatient();

        // Get patient medical records
        List<DossierMedical> medicalRecords = dossierMedicalService.getDossiersByPatientId(patient.getId());

        // Get previous consultations with this patient
        List<Consultation> previousConsultations = consultationService.getConsultationsByPatientAndMedecin(
                patient.getId(), medecin.getId());

        model.addAttribute("medecin", medecin);
        model.addAttribute("consultation", consultation);
        model.addAttribute("patient", patient);
        model.addAttribute("medicalRecords", medicalRecords);
        model.addAttribute("previousConsultations", previousConsultations);
        model.addAttribute("roomId", consultation.getCallRoomId());
        model.addAttribute("isActive", consultation.getIsActive() != null && consultation.getIsActive());

        return "medecin/consultation-room";
    }

    // POST: Doctor joins consultation
    @PostMapping("/join/{consultationId}")
    @ResponseBody
    public Map<String, Object> joinConsultation(@PathVariable Long consultationId, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        try {
            // Update consultation status to active
            consultation.setIsActive(Boolean.TRUE);
            consultation.setDate(new Date());
            consultationService.updateConsultation(consultation);

            // Send notification to patient - Use TimelineEventDTO
            TimelineEventDTO event = new TimelineEventDTO();
            event.setEventType("DOCTOR_JOINED");
            event.setConsultationId(consultationId);
            event.setUserId(medecin.getId());
            event.setDescription("Le médecin a rejoint la consultation");

            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                    event
            );

            response.put("success", true);
            response.put("roomId", consultation.getCallRoomId());
            response.put("patientId", consultation.getRendezVous().getPatient().getId());
            response.put("patientName", consultation.getRendezVous().getPatient().getName());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error joining consultation");
        }

        return response;
    }

    // POST: Doctor ends consultation - Use DTO
    @PostMapping("/end/{consultationId}")
    @ResponseBody
    public Map<String, Object> endConsultation(
            @PathVariable Long consultationId,
            @RequestBody ConsultationEndRequestDTO endRequest, // Use DTO
            HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        try {
            // Update consultation
            consultation.setIsActive(Boolean.FALSE);
            consultation.setNotes(endRequest.getNotes());
            consultationService.updateConsultation(consultation);

            // Update appointment status
            RendezVous appointment = consultation.getRendezVous();
            appointment.setStatus(StatusRendezVous.COMPLETED);
            rendezVousService.updateRendezVous(appointment);

            // Save prescription if provided
            if (endRequest.getPrescription() != null && !endRequest.getPrescription().isEmpty()) {
                Ordonance ordonance = new Ordonance();
                ordonance.setMedicaments(endRequest.getPrescription());
                ordonance.setDateCreation(new Date());
                ordonance.setConsultation(consultation);
                ordonance.setValideeParIA(false);
                ordonanceService.createOrdonance(ordonance);
            }

            // Save medical record if requested
            if (endRequest.isSaveAsMedicalRecord() && endRequest.getSummary() != null) {
                saveConsultationSummary(consultation, endRequest.getSummary());
            }

            // Send end notification to patient - Use TimelineEventDTO
            TimelineEventDTO event = new TimelineEventDTO();
            event.setEventType("CONSULTATION_ENDED");
            event.setConsultationId(consultationId);
            event.setUserId(medecin.getId());
            event.setDescription("La consultation est terminée");

            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                    event
            );

            response.put("success", true);
            response.put("message", "Consultation terminée avec succès");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error ending consultation");
        }

        return response;
    }

    // WebSocket: Doctor sends chat message - Use ChatMessageDTO
    @MessageMapping("/medecin/consultation/{consultationId}/chat")
    public void sendDoctorChatMessage(
            @Payload ChatMessageDTO message, // Use existing ChatMessageDTO
            @DestinationVariable Long consultationId) {

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation != null) {
            // Send to patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/chat",
                    message
            );

            // Also broadcast to consultation room
            messagingTemplate.convertAndSend(
                    "/topic/consultation/" + consultation.getCallRoomId() + "/chat",
                    message
            );
        }
    }

    // WebSocket: Doctor sends medical note - Create this DTO if missing
    @MessageMapping("/medecin/consultation/{consultationId}/notes")
    public void updateMedicalNotes(
            @Payload ConsultationNotesDTO notesUpdate, // Use ConsultationNotesDTO
            @DestinationVariable Long consultationId) {

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation != null) {
            // Update consultation notes
            String currentNotes = consultation.getNotes();
            if (currentNotes == null) {
                currentNotes = "";
            }
            consultation.setNotes(currentNotes + "\n" + notesUpdate.getNotes());
            consultationService.updateConsultation(consultation);

            // Send notification to patient about notes update
            messagingTemplate.convertAndSend(
                    "/topic/consultation/" + consultation.getCallRoomId() + "/notes",
                    notesUpdate
            );
        }
    }

    // WebSocket: Doctor sends file/document - Use FileShareMessageDTO
    @MessageMapping("/medecin/consultation/{consultationId}/share")
    public void shareFileWithPatient(
            @Payload FileShareMessageDTO fileMessage, // Use DTO
            @DestinationVariable Long consultationId) {

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation != null) {
            // Send to patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/files",
                    fileMessage
            );

            // Log the file share in consultation notes
            String note = "Fichier partagé: " + fileMessage.getFileName() +
                    " (" + fileMessage.getDescription() + ")";
            updateConsultationNotes(consultationId, note);
        }
    }

    // GET: Get consultation details for doctor
    @GetMapping("/details/{consultationId}")
    @ResponseBody
    public Map<String, Object> getConsultationDetails(@PathVariable Long consultationId, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
            response.put("success", false);
            return response;
        }

        // Get patient details
        Patient patient = consultation.getRendezVous().getPatient();

        // Get medical history
        List<DossierMedical> medicalHistory = dossierMedicalService.getDossiersByPatientId(patient.getId());

        response.put("success", true);
        response.put("consultation", consultation);
        response.put("patient", patient);
        response.put("medicalHistory", medicalHistory);
        response.put("appointment", consultation.getRendezVous());

        return response;
    }

    // POST: Create prescription - Use PrescriptionRequestDTO
    @PostMapping("/prescription/{consultationId}")
    @ResponseBody
    public Map<String, Object> createPrescription(
            @PathVariable Long consultationId,
            @RequestBody PrescriptionRequestDTO prescriptionRequest, // Use DTO
            HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        try {
            Ordonance ordonance = new Ordonance();
            ordonance.setMedicaments(prescriptionRequest.getMedicaments());
            // Note: Your Ordonance model doesn't have 'instructions' field
            // If you need it, add it to the model
            ordonance.setDateCreation(new Date());
            ordonance.setConsultation(consultation);
            ordonance.setValideeParIA(prescriptionRequest.isAiValidated());

            Ordonance savedOrdonance = ordonanceService.createOrdonance(ordonance);

            // Update consultation
            consultation.setOrdonance(savedOrdonance);
            consultationService.updateConsultation(consultation);

            // Notify patient - Create simple notification DTO
            Map<String, Object> notification = new HashMap<>();
            notification.put("prescriptionId", savedOrdonance.getId());
            notification.put("doctorName", medecin.getName());
            notification.put("createdAt", new Date());

            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/prescription",
                    notification
            );

            response.put("success", true);
            response.put("prescriptionId", savedOrdonance.getId());

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error creating prescription");
        }

        return response;
    }

    // GET: Waiting patients for doctor
    @GetMapping("/waiting")
    @ResponseBody
    public Map<String, Object> getWaitingPatients(HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            return response;
        }

        List<Consultation> waitingConsultations = consultationService.getWaitingConsultationsForMedecin(medecin.getId());

        List<Map<String, Object>> waitingPatients = new ArrayList<>();
        for (Consultation consultation : waitingConsultations) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("consultationId", consultation.getId());
            patientInfo.put("patientId", consultation.getRendezVous().getPatient().getId());
            patientInfo.put("patientName", consultation.getRendezVous().getPatient().getName());

            // Note: Your RendezVous has LocalDate date and String time separately
            RendezVous rdv = consultation.getRendezVous();
            if (rdv.getDate() != null && rdv.getTime() != null) {
                patientInfo.put("appointmentTime", rdv.getDate() + " " + rdv.getTime());
            } else {
                patientInfo.put("appointmentTime", "Non spécifié");
            }

            patientInfo.put("consultationType", consultation.getConsultationType());

            waitingPatients.add(patientInfo);
        }

        response.put("success", true);
        response.put("waitingPatients", waitingPatients);

        return response;
    }

    // POST: Notify patient that doctor is ready
    @PostMapping("/notify-ready/{consultationId}")
    @ResponseBody
    public Map<String, Object> notifyPatientReady(@PathVariable Long consultationId, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null || !consultation.getRendezVous().getMedecin().getId().equals(medecin.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        try {
            // Send ready notification to patient - Create simple notification DTO
            Map<String, Object> notification = new HashMap<>();
            notification.put("doctorId", medecin.getId());
            notification.put("doctorName", medecin.getName());
            notification.put("consultationId", consultationId);
            notification.put("notifiedAt", new Date());

            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/notification",
                    notification
            );

            response.put("success", true);
            response.put("message", "Patient notified");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error notifying patient");
        }

        return response;
    }

    // Helper methods
    private void saveConsultationSummary(Consultation consultation, String summary) {
        try {
            Patient patient = consultation.getRendezVous().getPatient();
            Medecin medecin = consultation.getRendezVous().getMedecin();

            // Create medical record entry using your DossierMedical model
            DossierMedical dossier = new DossierMedical();
            dossier.setPatient(patient);
            dossier.setTitle("Consultation du " + new Date());

            // Build description from consultation
            String description = buildConsultationDescription(consultation, summary, medecin.getName());
            dossier.setDescription(description);

            // Set file info if available
            if (consultation.getVideoURL() != null) {
                dossier.setFileName("consultation_video.mp4");
                dossier.setFileUrl(consultation.getVideoURL());
            }

            dossier.setUploadDate(LocalDateTime.now());

            dossierMedicalService.save(dossier);

        } catch (Exception e) {
            System.err.println("Error saving consultation summary: " + e.getMessage());
        }
    }

    private String buildConsultationDescription(Consultation consultation, String summary, String doctorName) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMPTE RENDU DE CONSULTATION ===\n\n");
        sb.append("Médecin: ").append(doctorName).append("\n");
        sb.append("Date: ").append(consultation.getDate()).append("\n");
        sb.append("Type: ").append(consultation.getConsultationType()).append("\n\n");

        sb.append("=== OBSERVATIONS ===\n");
        sb.append(consultation.getNotes() != null ? consultation.getNotes() : "Aucune observation").append("\n\n");

        sb.append("=== CONCLUSION ===\n");
        sb.append(summary).append("\n\n");

        if (consultation.getOrdonance() != null) {
            sb.append("=== ORDONNANCE ===\n");
            Ordonance ord = consultation.getOrdonance();
            if (ord.getMedicaments() != null) {
                for (String medicament : ord.getMedicaments()) {
                    sb.append("- ").append(medicament).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private void updateConsultationNotes(Long consultationId, String note) {
        try {
            Consultation consultation = consultationService.getConsultationById(consultationId);
            if (consultation != null) {
                String currentNotes = consultation.getNotes();
                if (currentNotes == null) {
                    currentNotes = "";
                }
                consultation.setNotes(currentNotes + "\n[" + new Date() + "] " + note);
                consultationService.updateConsultation(consultation);
            }
        } catch (Exception e) {
            System.err.println("Error updating consultation notes: " + e.getMessage());
        }
    }
}

// REMOVE ALL DTO CLASSES FROM HERE - THEY SHOULD BE IN THE DTO PACKAGE