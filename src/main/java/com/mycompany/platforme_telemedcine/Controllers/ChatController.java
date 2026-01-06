package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.MedecinRepository;
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
    private final MedecinRepository medecinRepository;

    // Track online users
    private final Map<Long, String> onlineUsers = new HashMap<>();

    @Autowired
    public ChatController(MessagerieService messagerieService,
                          PatientService patientService,
                          MedecinService medecinService,
                          SimpMessagingTemplate messagingTemplate,
                          NotificationService notificationService,
                          MedecinRepository medecinRepository) {
        this.messagerieService = messagerieService;
        this.patientService = patientService;
        this.medecinService = medecinService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.medecinRepository = medecinRepository;
    }

    // GET: Chat interface with doctor
// GET: Chat interface with doctor
    @GetMapping("/{doctorId}")
    public String chatWithDoctor(@PathVariable Long doctorId,
                                 HttpSession session,
                                 Model model) {

        // FIX: Check user type before casting
        Object user = session.getAttribute("user");

        // If it's a doctor, redirect to doctor chat
        if (user instanceof Medecin) {
            Medecin medecin = (Medecin) user;
            System.out.println("DEBUG: Doctor " + medecin.getName() + " trying to access patient chat. Redirecting...");
            return "redirect:/medecin/chat"; // Redirect to doctor chat
        }

        // Now it's safe to cast to Patient
        Patient patient = (Patient) user;
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

    // In ChatController.java, update chatDashboard method:
    @GetMapping
    public String chatDashboard(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<Medecin> doctors = medecinService.getApprovedDoctors();

        // Create a list with doctor info and last message
        List<Map<String, Object>> doctorWithLastMessage = new ArrayList<>();

        for (Medecin doctor : doctors) {
            Map<String, Object> doctorInfo = new HashMap<>();
            doctorInfo.put("doctor", doctor);

            // Get last message
            List<Messagerie> messages = messagerieService.getMessagesBetween(patient.getId(), doctor.getId());
            Messagerie lastMessage = null;
            if (!messages.isEmpty()) {
                // Sort by timestamp descending to get most recent
                messages.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));
                lastMessage = messages.get(0);
            }

            doctorInfo.put("lastMessage", lastMessage);
            doctorInfo.put("unreadCount", getUnreadCountForDoctor(patient.getId(), doctor.getId()));
            doctorInfo.put("isOnline", onlineUsers.containsKey(doctor.getId()));

            doctorWithLastMessage.add(doctorInfo);
        }

        // Sort doctors by last message timestamp (most recent first)
        doctorWithLastMessage.sort((d1, d2) -> {
            Messagerie msg1 = (Messagerie) d1.get("lastMessage");
            Messagerie msg2 = (Messagerie) d2.get("lastMessage");

            if (msg1 == null && msg2 == null) return 0;
            if (msg1 == null) return 1; // No message goes to bottom
            if (msg2 == null) return -1; // Has message goes to top

            return msg2.getTimestamp().compareTo(msg1.getTimestamp()); // Recent first
        });

        model.addAttribute("patient", patient);
        model.addAttribute("doctorsWithInfo", doctorWithLastMessage);
        model.addAttribute("doctors", doctors); // Keep original for compatibility

        return "patient/chat";
    }
    // Helper method
    private int getUnreadCountForDoctor(Long patientId, Long doctorId) {
        List<Messagerie> messages = messagerieService.getMessagesBetween(patientId, doctorId);
        return (int) messages.stream()
                .filter(m -> !m.isRead() && m.getReceiverId().equals(patientId))
                .count();
    }

    // WebSocket: Send message to doctor
    // WebSocket: Send message to doctor
    // WebSocket: Send message to doctor
    @MessageMapping("/chat.send/{doctorId}")
    public void sendMessageToDoctor(@DestinationVariable Long doctorId,
                                    @Payload ChatMessageDTO chatMessageDTO) {

        Patient patient = patientService.getPatientById(chatMessageDTO.getSenderId());
        Medecin doctor = medecinService.getMedecinById(doctorId);

        if (patient == null || doctor == null) {
            System.err.println("ERROR: Patient or Doctor not found!");
            return;
        }

        if (chatMessageDTO.getContent() == null || chatMessageDTO.getContent().trim().isEmpty()) {
            System.out.println("ERROR: Attempted to send NULL or empty message!");
            return;
        }

        // FIX: Use correct constructor with timestamp
        Messagerie message = new Messagerie(
                patient.getId(),
                doctorId,
                patient.getName(),
                doctor.getName(),
                "PATIENT",
                chatMessageDTO.getContent().trim(),
                LocalDateTime.now(),  // THIS MUST BE SET!
                false
        );

        Messagerie savedMessage = messagerieService.saveMessage(message);

        // FIX: Check for null timestamp
        String timestampStr = savedMessage.getTimestamp() != null
                ? savedMessage.getTimestamp().toString()
                : LocalDateTime.now().toString();

        // Create a proper response DTO
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedMessage.getId());
        response.put("senderId", savedMessage.getSenderId());
        response.put("receiverId", savedMessage.getReceiverId());
        response.put("senderName", savedMessage.getSenderName());
        response.put("senderType", savedMessage.getSenderRole());
        response.put("content", savedMessage.getContent());
        response.put("timestamp", timestampStr);  // Use safe timestamp
        response.put("isRead", savedMessage.isRead());

        // Send to receiver (doctor)
        messagingTemplate.convertAndSendToUser(
                doctorId.toString(),
                "/queue/messages",
                response
        );

        // Send to sender (patient) for confirmation
        messagingTemplate.convertAndSendToUser(
                patient.getId().toString(),
                "/queue/messages",
                response
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
                                 @Payload TypingStatusDTO typingStatusDTO) {

        messagingTemplate.convertAndSend(
                "/user/" + doctorId + "/queue/typing",
                typingStatusDTO
        );
    }

    // WebSocket: User connected
    @MessageMapping("/chat.connect")
    public void userConnected(@Payload UserConnectionDTO userConnectionDTO) {
        onlineUsers.put(userConnectionDTO.getUserId(), userConnectionDTO.getSessionId());

        // Broadcast online status to all connected users
        broadcastUserStatus(userConnectionDTO.getUserId(), true);
    }

    // WebSocket: User disconnected
    @MessageMapping("/chat.disconnect")
    public void userDisconnected(@Payload UserConnectionDTO userConnectionDTO) {
        onlineUsers.remove(userConnectionDTO.getUserId());

        // Broadcast offline status
        broadcastUserStatus(userConnectionDTO.getUserId(), false);
    }

    // WebSocket: Mark messages as read
    @MessageMapping("/chat.read/{doctorId}")
    public void markMessagesAsRead(@DestinationVariable Long doctorId,
                                   @Payload ReadStatusDTO readStatusDTO) {

        messagerieService.markMessagesAsRead(doctorId, readStatusDTO.getUserId());

        // Notify sender that messages were read
        messagingTemplate.convertAndSend(
                "/user/" + doctorId + "/queue/read",
                readStatusDTO
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

    // TEST ENDPOINTS (keep as is)
    @GetMapping("/test")
    @ResponseBody
    public String testDoctors() {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Doctor Repository Test</h2>");

        try {
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