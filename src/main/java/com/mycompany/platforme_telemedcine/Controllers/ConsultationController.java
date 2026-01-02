package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

@Controller
@RequestMapping("/consultation")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final RendezVousService rendezVousService;
    private final OrdonanceService ordonanceService;
    private final DossierMedicalService dossierMedicalService;
    private final NotificationService notificationService;
    private final PatientService patientService;
    private final MedecinService medecinService;
    private final SimpMessagingTemplate messagingTemplate;

    // Track active consultations and waiting rooms
    private final Map<String, ConsultationSession> activeSessions = new HashMap<>();
    private final Map<String, WaitingRoom> waitingRooms = new HashMap<>();

    @Autowired
    public ConsultationController(ConsultationService consultationService,
                                  RendezVousService rendezVousService,
                                  OrdonanceService ordonanceService,
                                  DossierMedicalService dossierMedicalService,
                                  NotificationService notificationService,
                                  PatientService patientService,
                                  MedecinService medecinService,
                                  SimpMessagingTemplate messagingTemplate) {
        this.consultationService = consultationService;
        this.rendezVousService = rendezVousService;
        this.ordonanceService = ordonanceService;
        this.dossierMedicalService = dossierMedicalService;
        this.notificationService = notificationService;
        this.patientService = patientService;
        this.medecinService = medecinService;
        this.messagingTemplate = messagingTemplate;
    }

    // GET: Consultation room
    @GetMapping("/{consultationId}")
    public String consultationRoom(@PathVariable Long consultationId,
                                   HttpSession session,
                                   Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/appointments?error=invalidConsultation";
        }

        Medecin doctor = consultation.getRendezVous().getMedecin();

        // Check if consultation is active
        boolean isActive = consultation.getActive() != null && consultation.getActive();

        // Get or create session
        String roomId = consultation.getCallRoomId();
        if (roomId == null) {
            roomId = "consultation_" + consultationId + "_" + System.currentTimeMillis();
            consultation.setCallRoomId(roomId);
            consultationService.updateConsultation(consultation);
        }

        // Create waiting room if not exists
        if (!waitingRooms.containsKey(roomId)) {
            waitingRooms.put(roomId, new WaitingRoom(roomId, consultationId));
        }

        // Add patient to waiting room
        waitingRooms.get(roomId).addParticipant(patient.getId(), patient.getName(), "PATIENT");

        model.addAttribute("consultation", consultation);
        model.addAttribute("patient", patient);
        model.addAttribute("doctor", doctor);
        model.addAttribute("roomId", roomId);
        model.addAttribute("isActive", isActive);
        model.addAttribute("consultationType", consultation.getConsultationType());
        model.addAttribute("isInWaitingRoom", !isActive);

        // Add SimpleWebRTC configuration
        model.addAttribute("stunServers", getStunServers());
        model.addAttribute("turnServers", getTurnServers());

        return "patient/consultation-room";
    }

    // POST: Join consultation (move from waiting room to active session)
    @PostMapping("/join/{consultationId}")
    @ResponseBody
    public Map<String, Object> joinConsultation(@PathVariable Long consultationId,
                                                HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (patient == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        String roomId = consultation.getCallRoomId();

        // Remove from waiting room
        if (waitingRooms.containsKey(roomId)) {
            waitingRooms.get(roomId).removeParticipant(patient.getId());

            // Notify doctor that patient has joined waiting room
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/consultation",
                    new ConsultationEvent("PATIENT_WAITING", consultationId, patient.getId())
            );
        }

        response.put("success", true);
        response.put("roomId", roomId);
        response.put("consultationType", consultation.getConsultationType());
        return response;
    }

    // WebSocket: Start consultation (doctor initiates)
    @MessageMapping("/consultation.start")
    public void startConsultation(@Payload ConsultationStartRequest request) {
        Consultation consultation = consultationService.getConsultationById(request.getConsultationId());
        if (consultation != null) {
            consultation.setActive(true);
            consultation.setDate(new Date());
            consultationService.updateConsultation(consultation);

            String roomId = consultation.getCallRoomId();

            // Create active session
            ConsultationSession session = new ConsultationSession(
                    roomId,
                    consultation.getId(),
                    consultation.getRendezVous().getPatient().getId(),
                    consultation.getRendezVous().getMedecin().getId(),
                    consultation.getConsultationType()
            );

            activeSessions.put(roomId, session);

            // Clear waiting room
            waitingRooms.remove(roomId);

            // Notify patient
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                    new ConsultationEvent("CONSULTATION_STARTED", consultation.getId(), null)
            );

            // Notify doctor
            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/consultation",
                    new ConsultationEvent("CONSULTATION_STARTED", consultation.getId(), null)
            );
        }
    }

    // WebSocket: End consultation
    @MessageMapping("/consultation.end")
    public void endConsultation(@Payload ConsultationEndRequest request) {
        String roomId = request.getRoomId();
        ConsultationSession session = activeSessions.get(roomId);

        if (session != null) {
            Consultation consultation = consultationService.getConsultationById(session.getConsultationId());
            if (consultation != null) {
                consultation.setActive(false);
                consultation.setNotes(request.getNotes());
                consultationService.updateConsultation(consultation);

                // Update appointment status
                RendezVous appointment = consultation.getRendezVous();
                appointment.setStatus(StatusRendezVous.COMPLETED);
                rendezVousService.updateRendezVous(appointment);

                // Save consultation as medical record if requested
                if (request.isSaveAsMedicalRecord()) {
                    saveConsultationAsMedicalRecord(consultation, request.getSummary());
                }

                // Generate prescription if requested
                if (request.getPrescription() != null && !request.getPrescription().isEmpty()) {
                    generatePrescription(consultation, request.getPrescription());
                }

                // Remove active session
                activeSessions.remove(roomId);

                // Send end notification to both parties
                ConsultationEvent endEvent = new ConsultationEvent(
                        "CONSULTATION_ENDED",
                        consultation.getId(),
                        request.getEndedBy()
                );
                endEvent.setDuration(session.getDuration());
                endEvent.setNotes(request.getNotes());

                messagingTemplate.convertAndSend(
                        "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                        endEvent
                );

                messagingTemplate.convertAndSend(
                        "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/consultation",
                        endEvent
                );
            }
        }
    }

    // WebSocket: Send signal (WebRTC signaling)
    @MessageMapping("/consultation.signal")
    public void handleSignal(@Payload SignalMessage signal) {
        messagingTemplate.convertAndSend(
                "/user/" + signal.getTargetUserId() + "/queue/signal",
                signal
        );
    }

    // WebSocket: Send chat message during consultation
    @MessageMapping("/consultation.chat")
    public void sendConsultationChat(@Payload ConsultationChatMessage message) {
        ConsultationSession session = activeSessions.get(message.getRoomId());
        if (session != null) {
            messagingTemplate.convertAndSend(
                    "/topic/consultation/" + message.getRoomId() + "/chat",
                    message
            );

            // Save chat message to consultation notes
            if (session.getChatHistory() == null) {
                session.setChatHistory(new ArrayList<>());
            }
            session.getChatHistory().add(message);
        }
    }

    // WebSocket: Share file during consultation
    @MessageMapping("/consultation.share")
    public void shareFile(@Payload FileShareMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/consultation/" + message.getRoomId() + "/files",
                message
        );
    }

    // WebSocket: Update consultation notes
    @MessageMapping("/consultation.notes")
    public void updateNotes(@Payload ConsultationNotes notes) {
        ConsultationSession session = activeSessions.get(notes.getRoomId());
        if (session != null) {
            session.setMedicalNotes(notes.getNotes());

            // Broadcast notes update to doctor
            messagingTemplate.convertAndSend(
                    "/user/" + session.getDoctorId() + "/queue/notes",
                    notes
            );
        }
    }

    // REST API: Upload file during consultation
    @PostMapping("/upload/{consultationId}")
    @ResponseBody
    public Map<String, Object> uploadConsultationFile(@PathVariable Long consultationId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam("description") String description,
                                                      HttpSession session) throws IOException {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (patient == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        // Save file to dossier medical
        DossierMedical dossier = dossierMedicalService.uploadDocumentMedical(
                patient,
                file,
                "Document de consultation: " + description,
                "Partagé pendant la consultation #" + consultationId
        );

        // Notify doctor about file upload
        FileShareMessage fileMessage = new FileShareMessage();
        fileMessage.setRoomId(consultation.getCallRoomId());
        fileMessage.setFileName(file.getOriginalFilename());
        fileMessage.setFileUrl(dossier.getFileUrl());
        fileMessage.setDescription(description);
        fileMessage.setSenderId(patient.getId());
        fileMessage.setSenderName(patient.getName());

        messagingTemplate.convertAndSend(
                "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/files",
                fileMessage
        );

        response.put("success", true);
        response.put("fileUrl", dossier.getFileUrl());
        response.put("fileName", file.getOriginalFilename());
        return response;
    }

    // REST API: Get consultation summary
    @GetMapping("/summary/{consultationId}")
    @ResponseBody
    public ConsultationSummary getConsultationSummary(@PathVariable Long consultationId,
                                                      HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return null;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return null;
        }

        ConsultationSession sessionData = activeSessions.get(consultation.getCallRoomId());

        ConsultationSummary summary = new ConsultationSummary();
        summary.setConsultationId(consultationId);
        summary.setDate(consultation.getDate());
        summary.setDoctorName(consultation.getRendezVous().getMedecin().getName());
        summary.setConsultationType(consultation.getConsultationType());
        summary.setNotes(consultation.getNotes());

        if (sessionData != null) {
            summary.setDuration(sessionData.getDuration());
            summary.setChatHistory(sessionData.getChatHistory());
            summary.setMedicalNotes(sessionData.getMedicalNotes());
        }

        // Get prescription if exists
        if (consultation.getOrdonance() != null) {
            summary.setPrescription(consultation.getOrdonance().getMedicaments());
        }

        return summary;
    }

    // REST API: Generate prescription
    @PostMapping("/prescription/{consultationId}")
    @ResponseBody
    public Map<String, Object> generatePrescription(@PathVariable Long consultationId,
                                                    @RequestBody List<String> medicaments,
                                                    HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (patient == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        // Create prescription
        Ordonance prescription = new Ordonance();
        prescription.setMedicaments(medicaments);
        prescription.setDateCreation(new Date());
        prescription.setValideeParIA(false); // Mark as not AI-validated
        prescription.setConsultation(consultation);

        Ordonance savedPrescription = ordonanceService.createOrdonance(prescription);

        // Update consultation with prescription
        consultation.setOrdonance(savedPrescription);
        consultationService.updateConsultation(consultation);

        response.put("success", true);
        response.put("prescriptionId", savedPrescription.getId());
        return response;
    }

    // REST API: Get waiting room status
    @GetMapping("/waiting-room/{roomId}")
    @ResponseBody
    public WaitingRoom getWaitingRoomStatus(@PathVariable String roomId,
                                            HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return null;
        }

        return waitingRooms.get(roomId);
    }

    // Helper methods
    private void saveConsultationAsMedicalRecord(Consultation consultation, String summary) {
        try {
            Patient patient = consultation.getRendezVous().getPatient();

            // Create a text file with consultation summary
            String fileName = "consultation_" + consultation.getId() + "_" +
                    consultation.getDate().getTime() + ".txt";
            String content = buildConsultationSummary(consultation, summary);

            // In a real implementation, you would save this as a file
            // and create a DossierMedical entry

            System.out.println("Consultation saved as medical record: " + fileName);

        } catch (Exception e) {
            System.err.println("Error saving consultation as medical record: " + e.getMessage());
        }
    }

    private void generatePrescription(Consultation consultation, List<String> medicaments) {
        Ordonance prescription = new Ordonance();
        prescription.setMedicaments(medicaments);
        prescription.setDateCreation(new Date());
        prescription.setValideeParIA(false);
        prescription.setConsultation(consultation);

        ordonanceService.createOrdonance(prescription);
    }

    private String buildConsultationSummary(Consultation consultation, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RÉSUMÉ DE CONSULTATION ===\n\n");
        sb.append("ID Consultation: ").append(consultation.getId()).append("\n");
        sb.append("Date: ").append(consultation.getDate()).append("\n");
        sb.append("Type: ").append(consultation.getConsultationType()).append("\n");
        sb.append("Médecin: ").append(consultation.getRendezVous().getMedecin().getName()).append("\n");
        sb.append("Patient: ").append(consultation.getRendezVous().getPatient().getName()).append("\n\n");
        sb.append("=== NOTES ===\n");
        sb.append(consultation.getNotes()).append("\n\n");
        sb.append("=== RÉSUMÉ ===\n");
        sb.append(summary).append("\n");

        if (consultation.getOrdonance() != null) {
            sb.append("\n=== ORDONNANCE ===\n");
            for (String medicament : consultation.getOrdonance().getMedicaments()) {
                sb.append("- ").append(medicament).append("\n");
            }
        }

        return sb.toString();
    }

    private List<String> getStunServers() {
        return Arrays.asList(
                "stun:stun.l.google.com:19302",
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302",
                "stun:stun3.l.google.com:19302",
                "stun:stun4.l.google.com:19302"
        );
    }

    private List<TurnServer> getTurnServers() {
        // In production, use real TURN servers with credentials
        return Arrays.asList(
                new TurnServer("turn:turn.example.com", "username", "password")
        );
    }
}

// Helper Classes

class ConsultationSession {
    private String roomId;
    private Long consultationId;
    private Long patientId;
    private Long doctorId;
    private ConsultationType consultationType;
    private Date startTime;
    private List<ConsultationChatMessage> chatHistory;
    private String medicalNotes;

    public ConsultationSession(String roomId, Long consultationId, Long patientId,
                               Long doctorId, ConsultationType consultationType) {
        this.roomId = roomId;
        this.consultationId = consultationId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.consultationType = consultationType;
        this.startTime = new Date();
        this.chatHistory = new ArrayList<>();
    }

    public long getDuration() {
        if (startTime == null) return 0;
        return (System.currentTimeMillis() - startTime.getTime()) / 1000; // in seconds
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public ConsultationType getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationType consultationType) { this.consultationType = consultationType; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public List<ConsultationChatMessage> getChatHistory() { return chatHistory; }
    public void setChatHistory(List<ConsultationChatMessage> chatHistory) { this.chatHistory = chatHistory; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
}

class WaitingRoom {
    private String roomId;
    private Long consultationId;
    private Map<Long, WaitingParticipant> participants;

    public WaitingRoom(String roomId, Long consultationId) {
        this.roomId = roomId;
        this.consultationId = consultationId;
        this.participants = new HashMap<>();
    }

    public void addParticipant(Long userId, String name, String role) {
        participants.put(userId, new WaitingParticipant(userId, name, role, new Date()));
    }

    public void removeParticipant(Long userId) {
        participants.remove(userId);
    }

    public List<WaitingParticipant> getParticipants() {
        return new ArrayList<>(participants.values());
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }
}

class WaitingParticipant {
    private Long userId;
    private String name;
    private String role;
    private Date joinedAt;

    public WaitingParticipant(Long userId, String name, String role, Date joinedAt) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public Date getJoinedAt() { return joinedAt; }
}

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
