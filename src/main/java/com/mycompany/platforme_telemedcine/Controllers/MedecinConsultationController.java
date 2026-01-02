package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

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

        return "medecin/consultation-dashboard";
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
        model.addAttribute("isActive", consultation.getActive() != null && consultation.getActive());

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
            consultation.setActive(true);
            consultation.setDate(new Date());
            consultationService.updateConsultation(consultation);

            // Send notification to patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                    new ConsultationEvent("DOCTOR_JOINED", consultationId, medecin.getId())
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

    // POST: Doctor ends consultation
    @PostMapping("/end/{consultationId}")
    @ResponseBody
    public Map<String, Object> endConsultation(
            @PathVariable Long consultationId,
            @RequestBody ConsultationEndRequest endRequest,
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
            consultation.setActive(false);
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

            // Send end notification to patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                    new ConsultationEvent("CONSULTATION_ENDED", consultationId, medecin.getId())
            );

            response.put("success", true);
            response.put("message", "Consultation terminée avec succès");

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error ending consultation");
        }

        return response;
    }

    // WebSocket: Doctor sends chat message
    @MessageMapping("/medecin/consultation/{consultationId}/chat")
    public void sendDoctorChatMessage(
            @Payload MedecinChatMessage message,
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

    // WebSocket: Doctor sends medical note
    @MessageMapping("/medecin/consultation/{consultationId}/notes")
    public void updateMedicalNotes(
            @Payload MedicalNotesUpdate notesUpdate,
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

    // WebSocket: Doctor sends file/document
    @MessageMapping("/medecin/consultation/{consultationId}/share")
    public void shareFileWithPatient(
            @Payload FileShareMessage fileMessage,
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

    // POST: Create prescription
    @PostMapping("/prescription/{consultationId}")
    @ResponseBody
    public Map<String, Object> createPrescription(
            @PathVariable Long consultationId,
            @RequestBody PrescriptionRequest prescriptionRequest,
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
            ordonance.setInstructions(prescriptionRequest.getInstructions());
            ordonance.setDateCreation(new Date());
            ordonance.setConsultation(consultation);
            ordonance.setMedecin(medecin);
            ordonance.setValideeParIA(prescriptionRequest.isAiValidated());

            Ordonance savedOrdonance = ordonanceService.createOrdonance(ordonance);

            // Update consultation
            consultation.setOrdonance(savedOrdonance);
            consultationService.updateConsultation(consultation);

            // Notify patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/prescription",
                    new PrescriptionNotification(savedOrdonance.getId(), medecin.getName())
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
            patientInfo.put("appointmentTime", consultation.getRendezVous().getDateHeure());
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
            // Send ready notification to patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/notification",
                    new DoctorReadyNotification(medecin.getId(), medecin.getName(), consultationId)
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

            String recordContent = buildConsultationRecord(consultation, summary, medecin.getName());

            // Create medical record entry
            DossierMedical dossier = new DossierMedical();
            dossier.setPatient(patient);
            dossier.setTitre("Consultation du " + new Date());
            dossier.setDescription(recordContent);
            dossier.setTypeDocument("CONSULTATION_SUMMARY");
            dossier.setDateCreation(new Date());

            dossierMedicalService.saveDossierMedical(dossier);

        } catch (Exception e) {
            System.err.println("Error saving consultation summary: " + e.getMessage());
        }
    }

    private String buildConsultationRecord(Consultation consultation, String summary, String doctorName) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMPTE RENDU DE CONSULTATION ===\n\n");
        sb.append("Médecin: ").append(doctorName).append("\n");
        sb.append("Date: ").append(new Date()).append("\n");
        sb.append("Type: ").append(consultation.getConsultationType()).append("\n");
        sb.append("Durée: ").append(consultation.getDuree() != null ? consultation.getDuree() + " minutes" : "N/A").append("\n\n");
        sb.append("=== MOTIF DE CONSULTATION ===\n");
        sb.append(consultation.getRendezVous().getMotif()).append("\n\n");
        sb.append("=== OBSERVATIONS ===\n");
        sb.append(consultation.getNotes() != null ? consultation.getNotes() : "Aucune observation").append("\n\n");
        sb.append("=== CONCLUSION ===\n");
        sb.append(summary).append("\n\n");

        if (consultation.getOrdonance() != null) {
            sb.append("=== ORDONNANCE ===\n");
            for (String medicament : consultation.getOrdonance().getMedicaments()) {
                sb.append("- ").append(medicament).append("\n");
            }
            if (consultation.getOrdonance().getInstructions() != null) {
                sb.append("\nInstructions: ").append(consultation.getOrdonance().getInstructions()).append("\n");
            }
        }

        sb.append("\n=== RECOMMANDATIONS ===\n");
        sb.append("Suivi recommandé dans ").append(consultation.getProchainRdv() != null ? consultation.getProchainRdv() : "à convenir");

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

// DTO Classes for Doctor Consultation

class MedecinChatMessage {
    private Long consultationId;
    private Long doctorId;
    private String doctorName;
    private String message;
    private Date timestamp;
    private String messageType; // TEXT, SYSTEM, WARNING

    public MedecinChatMessage() {
        this.timestamp = new Date();
        this.messageType = "TEXT";
    }

    // Getters and setters
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}

class MedicalNotesUpdate {
    private Long consultationId;
    private Long doctorId;
    private String notes;
    private Date updatedAt;

    public MedicalNotesUpdate() {
        this.updatedAt = new Date();
    }

    // Getters and setters
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

class PrescriptionRequest {
    private List<String> medicaments;
    private String instructions;
    private boolean aiValidated;

    // Getters and setters
    public List<String> getMedicaments() { return medicaments; }
    public void setMedicaments(List<String> medicaments) { this.medicaments = medicaments; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isAiValidated() { return aiValidated; }
    public void setAiValidated(boolean aiValidated) { this.aiValidated = aiValidated; }
}

class PrescriptionNotification {
    private Long prescriptionId;
    private String doctorName;
    private Date createdAt;

    public PrescriptionNotification(Long prescriptionId, String doctorName) {
        this.prescriptionId = prescriptionId;
        this.doctorName = doctorName;
        this.createdAt = new Date();
    }

    // Getters
    public Long getPrescriptionId() { return prescriptionId; }
    public String getDoctorName() { return doctorName; }
    public Date getCreatedAt() { return createdAt; }
}

class DoctorReadyNotification {
    private Long doctorId;
    private String doctorName;
    private Long consultationId;
    private Date notifiedAt;

    public DoctorReadyNotification(Long doctorId, String doctorName, Long consultationId) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.consultationId = consultationId;
        this.notifiedAt = new Date();
    }

    // Getters
    public Long getDoctorId() { return doctorId; }
    public String getDoctorName() { return doctorName; }
    public Long getConsultationId() { return consultationId; }
    public Date getNotifiedAt() { return notifiedAt; }
}

// Reuse existing DTOs from ConsultationController
// ConsultationEvent, ConsultationEndRequest, FileShareMessage


// DTO Classes

class ConsultationStartRequest {
    private Long consultationId;
    private Long doctorId;

    // Getters and setters
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
}

class ConsultationEndRequest {
    private String roomId;
    private Long endedBy;
    private String notes;
    private String summary;
    private List<String> prescription;
    private boolean saveAsMedicalRecord;

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getEndedBy() { return endedBy; }
    public void setEndedBy(Long endedBy) { this.endedBy = endedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getPrescription() { return prescription; }
    public void setPrescription(List<String> prescription) { this.prescription = prescription; }

    public boolean isSaveAsMedicalRecord() { return saveAsMedicalRecord; }
    public void setSaveAsMedicalRecord(boolean saveAsMedicalRecord) { this.saveAsMedicalRecord = saveAsMedicalRecord; }
}

class SignalMessage {
    private Long senderId;
    private Long targetUserId;
    private String roomId;
    private String type; // offer, answer, candidate
    private Object data;

    // Getters and setters
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}

class ConsultationChatMessage {
    private String roomId;
    private Long senderId;
    private String senderName;
    private String content;
    private Date timestamp;
    private String messageType; // TEXT, FILE

    public ConsultationChatMessage() {
        this.timestamp = new Date();
        this.messageType = "TEXT";
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
}

class FileShareMessage {
    private String roomId;
    private Long senderId;
    private String senderName;
    private String fileName;
    private String fileUrl;
    private String description;
    private long fileSize;

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
}

class ConsultationNotes {
    private String roomId;
    private Long updatedBy;
    private String notes;
    private Date updatedAt;

    public ConsultationNotes() {
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}

class ConsultationEvent {
    private String eventType;
    private Long consultationId;
    private Long initiatedBy;
    private String message;
    private Long duration; // in seconds
    private String notes;

    public ConsultationEvent(String eventType, Long consultationId, Long initiatedBy) {
        this.eventType = eventType;
        this.consultationId = consultationId;
        this.initiatedBy = initiatedBy;
        this.message = getEventMessage(eventType);
    }

    private String getEventMessage(String eventType) {
        switch (eventType) {
            case "PATIENT_WAITING":
                return "Patient en salle d'attente";
            case "CONSULTATION_STARTED":
                return "Consultation démarrée";
            case "CONSULTATION_ENDED":
                return "Consultation terminée";
            default:
                return "Événement de consultation";
        }
    }

    // Getters and setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(Long initiatedBy) { this.initiatedBy = initiatedBy; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

class ConsultationSummary {
    private Long consultationId;
    private Date date;
    private String doctorName;
    private ConsultationType consultationType;
    private String notes;
    private Long duration; // in seconds
    private List<ConsultationChatMessage> chatHistory;
    private String medicalNotes;
    private List<String> prescription;

    // Getters and setters
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public ConsultationType getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationType consultationType) { this.consultationType = consultationType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }

    public List<ConsultationChatMessage> getChatHistory() { return chatHistory; }
    public void setChatHistory(List<ConsultationChatMessage> chatHistory) { this.chatHistory = chatHistory; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }

    public List<String> getPrescription() { return prescription; }
    public void setPrescription(List<String> prescription) { this.prescription = prescription; }
}

class TurnServer {
    private String url;
    private String username;
    private String credential;

    public TurnServer(String url, String username, String credential) {
        this.url = url;
        this.username = username;
        this.credential = credential;
    }

    // Getters
    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getCredential() { return credential; }
}
