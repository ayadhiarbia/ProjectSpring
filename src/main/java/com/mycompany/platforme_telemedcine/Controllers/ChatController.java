package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.MedecinRepository;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient/chat")
public class ChatController {
    @Autowired
    private final MessagerieService messagerieService;
    @Autowired
    private final PatientService patientService;
    @Autowired
    private final MedecinService medecinService;
    @Autowired
    private final SimpMessagingTemplate messagingTemplate;
    @Autowired
    private final NotificationService notificationService;
    @Autowired
    private final MedecinRepository medecinRepository; // Added this

    // Track online users
    private final Map<Long, String> onlineUsers = new HashMap<>();

    @Autowired
    public ChatController(MessagerieService messagerieService,
                          PatientService patientService,
                          MedecinService medecinService,
                          SimpMessagingTemplate messagingTemplate,
                          NotificationService notificationService,
                          MedecinRepository medecinRepository) { // Added this parameter
        this.messagerieService = messagerieService;
        this.patientService = patientService;
        this.medecinService = medecinService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.medecinRepository = medecinRepository; // Initialize it
    }

    // GET: Chat interface with doctor
    @GetMapping("/{doctorId}")
    public String chatWithDoctor(@PathVariable Long doctorId,
                                 HttpSession session,
                                 Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Medecin doctor = medecinService.getMedecinById(doctorId);
        if (doctor == null) {
            return "redirect:/patient/dashboard?error=doctorNotFound";
        }

        // Get conversation history
        List<Messagerie> messages = messagerieService.getMessagesBetween(patient.getId(), doctorId);

        // Get patient's doctors for sidebar
        List<Medecin> doctors = medecinService.getAllMedecin();

        model.addAttribute("patient", patient);
        model.addAttribute("doctor", doctor);
        model.addAttribute("messages", messages);
        model.addAttribute("doctors", doctors);
        model.addAttribute("isOnline", onlineUsers.containsKey(doctorId));

        return "patient/chat-room";
    }

       // Use the service, not the repository directly

        @GetMapping
        public String chatDashboard(HttpSession session, Model model) {
            // 1. Get current patient from session
            Patient patient = (Patient) session.getAttribute("user");
            if (patient == null) {
                return "redirect:/login";
            }

            // 2. Fetch approved doctors using your fixed service method
            List<Medecin> doctors = medecinService.getApprovedDoctors();

            // 3. Safety check: Ensure the list is never null
            if (doctors == null) {
                doctors = new ArrayList<>();
            }

            // 4. Add to model - MUST MATCH THE NAME IN YOUR HTML
            model.addAttribute("patient", patient);
            model.addAttribute("doctors", doctors); // This is what ${doctors} looks for

            return "patient/chat"; // Points to chat.html
        }


    // WebSocket: Send message to doctor
    @MessageMapping("/chat.send/{doctorId}")
    public void sendMessageToDoctor(@DestinationVariable Long doctorId,
                                    @Payload ChatMessage chatMessage) {

        Patient patient = patientService.getPatientById(chatMessage.getSenderId());
        Medecin doctor = medecinService.getMedecinById(doctorId);

        // Save message to database
        Messagerie message = new Messagerie(
                patient.getId(),
                doctorId,
                patient.getName(),
                doctor.getName(),
                "PATIENT",
                chatMessage.getContent(),
                LocalDateTime.now()
        );

        Messagerie savedMessage = messagerieService.saveMessage(message);

        // Send to receiver (doctor)
        messagingTemplate.convertAndSend(
                "/user/" + doctorId + "/queue/messages",
                savedMessage
        );

        // Send to sender (patient) for confirmation
        messagingTemplate.convertAndSend(
                "/user/" + patient.getId() + "/queue/messages",
                savedMessage
        );

        // Send notification to doctor if offline
        if (!onlineUsers.containsKey(doctorId)) {
            notificationService.createNotification(
                    "Nouveau message de " + patient.getName(),
                    doctorId
            );
        }
    }

    // WebSocket: Typing indicator
    @MessageMapping("/chat.typing/{doctorId}")
    public void sendTypingStatus(@DestinationVariable Long doctorId,
                                 @Payload TypingStatus typingStatus) {

        messagingTemplate.convertAndSend(
                "/user/" + doctorId + "/queue/typing",
                typingStatus
        );
    }

    // WebSocket: User connected
    @MessageMapping("/chat.connect")
    public void userConnected(@Payload UserConnection userConnection) {
        onlineUsers.put(userConnection.getUserId(), userConnection.getSessionId());

        // Broadcast online status to all connected users
        broadcastUserStatus(userConnection.getUserId(), true);
    }

    // WebSocket: User disconnected
    @MessageMapping("/chat.disconnect")
    public void userDisconnected(@Payload UserConnection userConnection) {
        onlineUsers.remove(userConnection.getUserId());

        // Broadcast offline status
        broadcastUserStatus(userConnection.getUserId(), false);
    }

    // WebSocket: Mark messages as read
    @MessageMapping("/chat.read/{doctorId}")
    public void markMessagesAsRead(@DestinationVariable Long doctorId,
                                   @Payload ReadStatus readStatus) {

        messagerieService.markMessagesAsRead(doctorId, readStatus.getUserId());

        // Notify sender that messages were read
        messagingTemplate.convertAndSend(
                "/user/" + doctorId + "/queue/read",
                readStatus
        );
    }

    // REST API: Get unread messages count
    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (patient == null) {
            response.put("count", 0);
            return response;
        }

        // Get unread messages count
        List<Messagerie> unreadMessages = messagerieService.getUnreadMessages(patient.getId());
        response.put("count", unreadMessages.size());
        response.put("patientId", patient.getId());

        return response;
    }

    // REST API: Search doctors
    @GetMapping("/search")
    @ResponseBody
    public List<Medecin> searchDoctors(@RequestParam String query, HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return Collections.emptyList();
        }

        List<Medecin> allDoctors = medecinService.getAllMedecin();
        List<Medecin> results = new ArrayList<>();

        for (Medecin doctor : allDoctors) {
            if (doctor.getName().toLowerCase().contains(query.toLowerCase()) ||
                    doctor.getPrenom().toLowerCase().contains(query.toLowerCase()) ||
                    doctor.getSpecialte().toLowerCase().contains(query.toLowerCase())) {
                results.add(doctor);
            }
        }

        return results;
    }

    // REST API: Get chat history
    @GetMapping("/history/{doctorId}")
    @ResponseBody
    public List<Messagerie> getChatHistory(@PathVariable Long doctorId, HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return Collections.emptyList();
        }

        return messagerieService.getMessagesBetween(patient.getId(), doctorId);
    }

    // Helper method to broadcast user status
    private void broadcastUserStatus(Long userId, boolean isOnline) {
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("userId", userId);
        statusUpdate.put("isOnline", isOnline);
        statusUpdate.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/user.status", statusUpdate);
    }

    // TEST ENDPOINTS
    @GetMapping("/test")
    @ResponseBody
    public String testDoctors() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Doctor Repository Test</h2>");

        try {
            // Test 1: Repository query
            sb.append("<h3>1. medecinRepository.findApprovedDoctors()</h3>");
            List<Medecin> repoDoctors = medecinRepository.findApprovedDoctors();
            sb.append("Count: ").append(repoDoctors.size()).append("<br>");

            if (repoDoctors.isEmpty()) {
                sb.append("<span style='color: red;'>NO APPROVED DOCTORS FOUND!</span><br>");
            } else {
                for (Medecin d : repoDoctors) {
                    sb.append("ID: ").append(d.getId())
                            .append(" | Name: ").append(d.getName())
                            .append(" | Status: ").append(d.getStatus())
                            .append(" | Status == APPROVED: ").append(d.getStatus() == UserStatus.APPROVED)
                            .append("<br>");
                }
            }

            // Test 2: All doctors
            sb.append("<h3>2. medecinRepository.findAll()</h3>");
            List<Medecin> allDoctors = medecinRepository.findAll();
            sb.append("Count: ").append(allDoctors.size()).append("<br>");

            if (allDoctors.isEmpty()) {
                sb.append("<span style='color: red;'>NO DOCTORS IN DATABASE!</span><br>");
            } else {
                for (Medecin d : allDoctors) {
                    sb.append("ID: ").append(d.getId())
                            .append(" | Name: ").append(d.getName())
                            .append(" | Email: ").append(d.getEmail())
                            .append(" | Status: ").append(d.getStatus())
                            .append(" | Status toString: '").append(d.getStatus() != null ? d.getStatus().toString() : "NULL").append("'")
                            .append("<br>");
                }
            }

            // Test 3: Service method
            sb.append("<h3>3. medecinService.getApprovedDoctors()</h3>");
            List<Medecin> serviceDoctors = medecinService.getApprovedDoctors();
            sb.append("Count: ").append(serviceDoctors.size()).append("<br>");

        } catch (Exception e) {
            sb.append("<h3 style='color: red;'>ERROR:</h3>");
            sb.append("<p>").append(e.getMessage()).append("</p>");
        }

        return sb.toString();
    }
    @GetMapping("/verify-data")
    @ResponseBody
    public String verifyData() {
        StringBuilder sb = new StringBuilder();

        // Test all doctors
        List<Medecin> all = medecinRepository.findAll();
        sb.append("All doctors in DB: ").append(all.size()).append("<br>");
        for (Medecin d : all) {
            sb.append("ID: ").append(d.getId())
                    .append(" | Name: ").append(d.getName())
                    .append(" | Email: ").append(d.getEmail())
                    .append(" | Status: ").append(d.getStatus())
                    .append(" | Status == APPROVED: ").append(d.getStatus() == UserStatus.APPROVED)
                    .append("<br>");
        }

        // Test approved doctors query
        List<Medecin> approved = medecinRepository.findApprovedDoctors();
        sb.append("<br>Approved doctors query: ").append(approved.size()).append("<br>");

        return sb.toString();
    }

    @GetMapping("/simple-test")
    @ResponseBody
    public Map<String, Object> simpleTest() {
        Map<String, Object> result = new HashMap<>();

        try {
            result.put("allDoctorsCount", medecinRepository.findAll().size());
            result.put("approvedDoctorsCount", medecinRepository.findApprovedDoctors().size());
            result.put("onlineUsersCount", onlineUsers.size());
            result.put("status", "SUCCESS");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }

        return result;
    }

    @GetMapping("/debug")
    @ResponseBody
    public String debug() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Chat System Debug</h2>");

        sb.append("<h3>Online Users:</h3>");
        sb.append("Count: ").append(onlineUsers.size()).append("<br>");
        for (Map.Entry<Long, String> entry : onlineUsers.entrySet()) {
            sb.append("User ID: ").append(entry.getKey())
                    .append(" | Session: ").append(entry.getValue())
                    .append("<br>");
        }

        return sb.toString();
    }
}

// DTO Classes for WebSocket communication

class ChatMessage {
    private Long senderId;
    private String content;
    private String messageType; // TEXT, IMAGE, FILE
    private String fileUrl; // Optional for file sharing

    // Getters and Setters
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}

class TypingStatus {
    private Long userId;
    private Long receiverId;
    private boolean isTyping;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }
}

class UserConnection {
    private Long userId;
    private String sessionId;
    private String userRole; // PATIENT or MEDECIN

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
}

class ReadStatus {
    private Long userId;
    private Long otherUserId;
    private LocalDateTime readAt;

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}