package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import com.mycompany.platforme_telemedcine.dto.*; // Import DTOs
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Controller
@RequestMapping("/medecin/chat")
public class MedecinChatController {

    @Autowired
    private MessagerieService messagerieService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedecinService medecinService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RendezVousService rendezVousService;

    // Track online users
    private final Map<Long, String> onlineUsers = new HashMap<>();

    // GET: Chat interface with patient
    @GetMapping("/{patientId}")
    public String chatWithPatient(@PathVariable Long patientId,
                                  HttpSession session,
                                  Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            return "redirect:/medecin/dashboard?error=patientNotFound";
        }

        // Get conversation history
        List<Messagerie> messages = messagerieService.getMessagesBetween(patientId, medecin.getId());

        // Get doctor's patients as DTOs
        List<PatientSimpleDTO> patientDTOs = getDoctorPatientsAsDTO(medecin.getId());

        // Get appointment history with this patient
        List<RendezVous> appointmentsWithPatient = getAppointmentsWithPatient(medecin.getId(), patientId);

        model.addAttribute("medecin", medecin);
        model.addAttribute("patient", patient);
        model.addAttribute("messages", messages);
        model.addAttribute("patientDTOs", patientDTOs);
        model.addAttribute("isOnline", onlineUsers.containsKey(patientId));
        model.addAttribute("unreadCount", getUnreadCountForPatient(medecin.getId(), patientId));
        model.addAttribute("appointments", appointmentsWithPatient);
        model.addAttribute("appointmentsCount", appointmentsWithPatient.size());

        return "medecin/chat-room";
    }

    // Main chat dashboard
    @GetMapping
    public String chatDashboard(HttpSession session, Model model) {
        System.out.println("=== chatDashboard called ===");
        Object userObj = session.getAttribute("user");

        if (userObj instanceof Medecin) {
            Medecin medecin = (Medecin) userObj;
            System.out.println("Doctor authenticated: " + medecin.getName() + " (ID: " + medecin.getId() + ")");

            // Get conversations using DTOs
            List<ConversationDTO> conversations = getConversationsForDoctor(medecin.getId());

            // CRITICAL: Add medecin to model
            model.addAttribute("medecin", medecin);
            model.addAttribute("conversations", conversations);
            model.addAttribute("showLoginPrompt", false);

            System.out.println("Returning chat with " + conversations.size() + " conversations");
            System.out.println("Doctor ID in model: " + medecin.getId());

            return "medecin/chat";

        } else {
            System.out.println("No doctor found in session. User object: " + userObj);
            model.addAttribute("showLoginPrompt", true);
            return "medecin/chat";
        }
    }

    // WebSocket: Send message to patient
    @MessageMapping("/medecin/chat.send/{patientId}")
    public void sendMessageToPatient(@DestinationVariable Long patientId,
                                     @Payload ChatMessageDTO chatMessageDTO) {

        Medecin medecin = medecinService.getMedecinById(chatMessageDTO.getSenderId());
        Patient patient = patientService.getPatientById(patientId);

        if (medecin == null || patient == null) {
            return;
        }

        // Save message to database
        Messagerie message = new Messagerie(
                medecin.getId(),
                patientId,
                medecin.getName(),
                patient.getName(),
                "MEDECIN",
                chatMessageDTO.getContent(),
                LocalDateTime.now(),
                false
        );

        Messagerie savedMessage = messagerieService.saveMessage(message);

        // Create response DTO
        Map<String, Object> response = createMessageResponse(savedMessage);

        // Send to receiver (patient)
        messagingTemplate.convertAndSendToUser(
                patientId.toString(),
                "/queue/messages",
                response
        );

        // Send to sender (doctor) for confirmation
        messagingTemplate.convertAndSendToUser(
                medecin.getId().toString(),
                "/queue/messages",
                response
        );

        // Send notification to patient if offline
        if (!onlineUsers.containsKey(patientId)) {
            notificationService.createNotification(
                    "Nouveau message de Dr. " + medecin.getName(),
                    patientId
            );
        }
    }

    // WebSocket: Typing indicator
    @MessageMapping("/medecin/chat.typing/{patientId}")
    public void sendTypingStatus(@DestinationVariable Long patientId,
                                 @Payload TypingStatusDTO typingStatusDTO) {

        Map<String, Object> response = new HashMap<>();
        response.put("userId", typingStatusDTO.getUserId());
        response.put("isTyping", typingStatusDTO.isTyping());

        messagingTemplate.convertAndSendToUser(
                patientId.toString(),
                "/queue/typing",
                response
        );
    }

    // WebSocket: Doctor connected
    @MessageMapping("/medecin/chat.connect")
    public void doctorConnected(@Payload UserConnectionDTO userConnectionDTO) {
        onlineUsers.put(userConnectionDTO.getUserId(), userConnectionDTO.getSessionId());

        // Broadcast doctor online status
        broadcastUserStatus(userConnectionDTO.getUserId(), true, "MEDECIN");
    }

    // WebSocket: Doctor disconnected
    @MessageMapping("/medecin/chat.disconnect")
    public void doctorDisconnected(@Payload UserConnectionDTO userConnectionDTO) {
        onlineUsers.remove(userConnectionDTO.getUserId());
        broadcastUserStatus(userConnectionDTO.getUserId(), false, "MEDECIN");
    }

    // WebSocket: Mark messages as read
    @MessageMapping("/medecin/chat.read/{patientId}")
    public void markMessagesAsRead(@DestinationVariable Long patientId,
                                   @Payload ReadStatusDTO readStatusDTO) {

        // Mark messages as read in database
        messagerieService.markDoctorMessagesAsRead(readStatusDTO.getUserId(), patientId);

        // Create read receipt
        Map<String, Object> readReceipt = new HashMap<>();
        readReceipt.put("doctorId", readStatusDTO.getUserId());
        readReceipt.put("patientId", patientId);
        readReceipt.put("readAt", LocalDateTime.now().toString());

        // Send read receipt to patient
        messagingTemplate.convertAndSendToUser(
                patientId.toString(),
                "/queue/read",
                readReceipt
        );
    }

    // REST API: Get unread messages count
    // In your getUnreadCount method (line 223 in error):
    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(HttpSession session) {
        Object userObj = session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (!(userObj instanceof Medecin)) {
            response.put("success", false);
            response.put("count", 0);
            return response;
        }

        Medecin medecin = (Medecin) userObj;
        int count = messagerieService.getUnreadMessagesForDoctor(medecin.getId()).size();
        response.put("success", true);
        response.put("count", count);
        return response;
    }

    // REST API: Search patients
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> searchPatients(@RequestParam String query, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("patients", new ArrayList<>());
            return response;
        }

        List<ConversationDTO> allConversations = getConversationsForDoctor(medecin.getId());
        List<ConversationDTO> results = new ArrayList<>();

        for (ConversationDTO conversation : allConversations) {
            if (conversation.getPatientName().toLowerCase().contains(query.toLowerCase()) ||
                    conversation.getPatientEmail().toLowerCase().contains(query.toLowerCase()) ||
                    (conversation.getPatientPrenom() != null &&
                            conversation.getPatientPrenom().toLowerCase().contains(query.toLowerCase()))) {
                results.add(conversation);
            }
        }

        response.put("success", true);
        response.put("patients", results);
        return response;
    }

    // REST API: Get chat history
    @GetMapping("/history/{patientId}")
    @ResponseBody
    public Map<String, Object> getChatHistory(@PathVariable Long patientId, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("messages", new ArrayList<>());
            return response;
        }

        List<Messagerie> messages = messagerieService.getMessagesBetween(patientId, medecin.getId());
        response.put("success", true);
        response.put("messages", messages);
        return response;
    }

    // REST API: Get recent conversations
    // In your getConversations method (line 291 in error):
    @GetMapping("/conversations")
    @ResponseBody
    public Map<String, Object> getConversations(HttpSession session) {
        Object userObj = session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (!(userObj instanceof Medecin)) {
            response.put("success", false);
            response.put("conversations", new ArrayList<>());
            return response;
        }

        Medecin medecin = (Medecin) userObj;
        List<ConversationDTO> conversations = getConversationsForDoctor(medecin.getId());
        response.put("success", true);
        response.put("conversations", conversations);
        return response;
    }

    // Helper method to get doctor's patients from appointments
    private List<Patient> getDoctorPatients(Long doctorId) {
        List<RendezVous> rendezVousList =
                rendezVousService.getRendezVousByMedecinId(doctorId)
                        .stream()
                        .filter(r ->
                                r.getStatus() == StatusRendezVous.APPROVED ||
                                        r.getStatus() == StatusRendezVous.COMPLETED
                        )
                        .collect(Collectors.toList());

        if (rendezVousList == null || rendezVousList.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> patientIds = new HashSet<>();
        List<Patient> patients = new ArrayList<>();

        for (RendezVous rendezVous : rendezVousList) {
            Patient patient = rendezVous.getPatient();
            if (patient != null && !patientIds.contains(patient.getId())) {
                patientIds.add(patient.getId());
                patients.add(patient);
            }
        }

        // Sort patients by name
        patients.sort(Comparator.comparing(
                p -> p.getName() != null ? p.getName().toLowerCase() : "",
                Comparator.nullsLast(String::compareTo)
        ));

        return patients;
    }

    // Helper method to get patients as simple DTOs
    private List<PatientSimpleDTO> getDoctorPatientsAsDTO(Long doctorId) {
        List<Patient> patients = getDoctorPatients(doctorId);
        List<PatientSimpleDTO> patientDTOs = new ArrayList<>();

        for (Patient patient : patients) {
            PatientSimpleDTO dto = new PatientSimpleDTO();
            dto.setId(patient.getId());
            dto.setName(patient.getName());
            dto.setPrenom(patient.getPrenom());
            dto.setEmail(patient.getEmail());
            patientDTOs.add(dto);
        }

        return patientDTOs;
    }

    // Helper method to get conversations as DTOs
    private List<ConversationDTO> getConversationsForDoctor(Long doctorId) {
        List<Patient> patients = getDoctorPatients(doctorId);
        List<ConversationDTO> conversations = new ArrayList<>();

        for (Patient patient : patients) {
            // Get last message
            List<Messagerie> messages = messagerieService.getMessagesBetween(patient.getId(), doctorId);
            Messagerie lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);

            // Create DTO
            ConversationDTO dto = new ConversationDTO();
            dto.setPatientId(patient.getId());
            dto.setPatientName(patient.getName());
            dto.setPatientPrenom(patient.getPrenom());
            dto.setPatientEmail(patient.getEmail());

            if (lastMessage != null) {
                dto.setLastMessageContent(lastMessage.getContent());
                dto.setLastMessageTimestamp(lastMessage.getTimestamp());
                dto.setLastMessageSenderRole(lastMessage.getSenderRole());
            }

            dto.setUnreadCount(getUnreadCountForPatient(doctorId, patient.getId()));
            dto.setOnline(onlineUsers.containsKey(patient.getId()));

            conversations.add(dto);
        }

        // Sort by last message timestamp (most recent first)
        conversations.sort((a, b) -> {
            LocalDateTime timeA = a.getLastMessageTimestamp();
            LocalDateTime timeB = b.getLastMessageTimestamp();
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });

        return conversations;
    }

    // Get appointments between doctor and specific patient
    private List<RendezVous> getAppointmentsWithPatient(Long doctorId, Long patientId) {
        List<RendezVous> allDoctorAppointments = rendezVousService.getRendezVousByMedecinId(doctorId);

        if (allDoctorAppointments == null) {
            return new ArrayList<>();
        }

        return allDoctorAppointments.stream()
                .filter(r -> r.getPatient() != null && r.getPatient().getId().equals(patientId))
                .sorted(Comparator.comparing(
                        RendezVous::getDate,
                        Comparator.nullsFirst(LocalDate::compareTo)
                ).reversed()) // Most recent first
                .collect(Collectors.toList());
    }

    // Get last message between doctor and patient
    private Messagerie getLastMessage(Long doctorId, Long patientId) {
        List<Messagerie> messages = messagerieService.getMessagesBetween(patientId, doctorId);
        if (messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    // Get unread message count for specific patient
    private int getUnreadCountForPatient(Long doctorId, Long patientId) {
        return messagerieService.getUnreadMessagesForDoctorFromPatient(doctorId, patientId).size();
    }

    // Broadcast user status to all connected users
    private void broadcastUserStatus(Long userId, boolean isOnline, String userType) {
        Map<String, Object> status = new HashMap<>();
        status.put("userId", userId);
        status.put("isOnline", isOnline);
        status.put("userType", userType);
        status.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/user.status", status);
    }

    // Create message response DTO
    private Map<String, Object> createMessageResponse(Messagerie message) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        response.put("senderId", message.getSenderId());
        response.put("receiverId", message.getReceiverId());
        response.put("senderName", message.getSenderName());
        response.put("senderType", message.getSenderRole());
        response.put("content", message.getContent());
        response.put("timestamp", message.getTimestamp().toString());
        response.put("isRead", message.isRead());
        return response;
    }

    // REST API: Get patient appointment statistics
    @GetMapping("/patient/{patientId}/stats")
    @ResponseBody
    public Map<String, Object> getPatientStats(@PathVariable Long patientId, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            return response;
        }

        List<RendezVous> appointments = getAppointmentsWithPatient(medecin.getId(), patientId);

        // Calculate statistics
        long totalAppointments = appointments.size();
        long completedAppointments = appointments.stream()
                .filter(a -> a.getStatus() == StatusRendezVous.COMPLETED)
                .count();
        long upcomingAppointments = appointments.stream()
                .filter(a -> a.getDate() != null &&
                        !a.getDate().isBefore(LocalDate.now()) &&
                        a.getStatus() == StatusRendezVous.PENDING)
                .count();

        RendezVous lastAppointment = appointments.stream()
                .filter(a -> a.getDate() != null)
                .max(Comparator.comparing(RendezVous::getDate))
                .orElse(null);

        response.put("success", true);
        response.put("totalAppointments", totalAppointments);
        response.put("completedAppointments", completedAppointments);
        response.put("upcomingAppointments", upcomingAppointments);
        response.put("lastAppointment", lastAppointment);

        return response;
    }

    // REST API: Send message to patient
    @PostMapping("/send/{patientId}")
    @ResponseBody
    public Map<String, Object> sendMessage(@PathVariable Long patientId,
                                           @RequestBody Map<String, String> messageData,
                                           HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("success", false);
            response.put("message", "Doctor not authenticated");
            return response;
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            response.put("success", false);
            response.put("message", "Patient not found");
            return response;
        }

        String content = messageData.get("content");
        if (content == null || content.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Message content is empty");
            return response;
        }

        try {
            // Save message to database
            Messagerie message = new Messagerie(
                    medecin.getId(),
                    patientId,
                    medecin.getName(),
                    patient.getName(),
                    "MEDECIN",
                    content.trim(),
                    LocalDateTime.now(),
                    false
            );

            Messagerie savedMessage = messagerieService.saveMessage(message);

            // Create response
            response.put("success", true);
            response.put("message", createMessageResponse(savedMessage));
            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error sending message: " + e.getMessage());
            return response;
        }
    }

    // DTO class for simple patient information
    public static class PatientSimpleDTO {
        private Long id;
        private String name;
        private String prenom;
        private String email;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // DTO class for conversation information
    public static class ConversationDTO {
        private Long patientId;
        private String patientName;
        private String patientPrenom;
        private String patientEmail;
        private String lastMessageContent;
        private LocalDateTime lastMessageTimestamp;
        private String lastMessageSenderRole;
        private int unreadCount;
        private boolean isOnline;

        // Getters and setters
        public Long getPatientId() { return patientId; }
        public void setPatientId(Long patientId) { this.patientId = patientId; }

        public String getPatientName() { return patientName; }
        public void setPatientName(String patientName) { this.patientName = patientName; }

        public String getPatientPrenom() { return patientPrenom; }
        public void setPatientPrenom(String patientPrenom) { this.patientPrenom = patientPrenom; }

        public String getPatientEmail() { return patientEmail; }
        public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

        public String getLastMessageContent() { return lastMessageContent; }
        public void setLastMessageContent(String lastMessageContent) { this.lastMessageContent = lastMessageContent; }

        public LocalDateTime getLastMessageTimestamp() { return lastMessageTimestamp; }
        public void setLastMessageTimestamp(LocalDateTime lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

        public String getLastMessageSenderRole() { return lastMessageSenderRole; }
        public void setLastMessageSenderRole(String lastMessageSenderRole) { this.lastMessageSenderRole = lastMessageSenderRole; }

        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

        public boolean isOnline() { return isOnline; }
        public void setOnline(boolean online) { isOnline = online; }
    }
}