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
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/medecin/chat")
public class MedecinChatController {

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

    // Track online users
    private final Map<Long, String> onlineUsers = new HashMap<>();

    @Autowired
    public MedecinChatController(MessagerieService messagerieService,
                                 PatientService patientService,
                                 MedecinService medecinService,
                                 SimpMessagingTemplate messagingTemplate,
                                 NotificationService notificationService) {
        this.messagerieService = messagerieService;
        this.patientService = patientService;
        this.medecinService = medecinService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

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

        // Get doctor's patients for sidebar
        List<Patient> patients = getDoctorPatients(medecin.getId());

        model.addAttribute("medecin", medecin);
        model.addAttribute("patient", patient);
        model.addAttribute("messages", messages);
        model.addAttribute("patients", patients);
        model.addAttribute("isOnline", onlineUsers.containsKey(patientId));

        return "medecin/chat-room";
    }

    // GET: Chat dashboard showing all patients
    @GetMapping
    public String chatDashboard(HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        // Get doctor's patients
        List<Patient> patients = getDoctorPatients(medecin.getId());

        model.addAttribute("medecin", medecin);
        model.addAttribute("patients", patients);

        return "medecin/chat";
    }

    // WebSocket: Send message to patient
    @MessageMapping("/medecin/chat.send/{patientId}")
    public void sendMessageToPatient(@DestinationVariable Long patientId,
                                     @Payload ChatMessage chatMessage) {

        Medecin medecin = medecinService.getMedecinById(chatMessage.getSenderId());
        Patient patient = patientService.getPatientById(patientId);

        // Save message to database
        Messagerie message = new Messagerie(
                patientId,
                medecin.getId(),
                patient.getName(),
                medecin.getName(),
                "MEDECIN",
                chatMessage.getContent(),
                LocalDateTime.now()
        );

        Messagerie savedMessage = messagerieService.saveMessage(message);

        // Send to receiver (patient)
        messagingTemplate.convertAndSend(
                "/user/" + patientId + "/queue/messages",
                savedMessage
        );

        // Send to sender (doctor) for confirmation
        messagingTemplate.convertAndSend(
                "/user/" + medecin.getId() + "/queue/messages",
                savedMessage
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
                                 @Payload TypingStatus typingStatus) {

        messagingTemplate.convertAndSend(
                "/user/" + patientId + "/queue/typing",
                typingStatus
        );
    }

    // WebSocket: Doctor connected
    @MessageMapping("/medecin/chat.connect")
    public void doctorConnected(@Payload UserConnection userConnection) {
        onlineUsers.put(userConnection.getUserId(), userConnection.getSessionId());

        // Broadcast online status to all connected users
        broadcastUserStatus(userConnection.getUserId(), true);
    }

    // WebSocket: Doctor disconnected
    @MessageMapping("/medecin/chat.disconnect")
    public void doctorDisconnected(@Payload UserConnection userConnection) {
        onlineUsers.remove(userConnection.getUserId());

        // Broadcast offline status
        broadcastUserStatus(userConnection.getUserId(), false);
    }

    // WebSocket: Mark messages as read
    @MessageMapping("/medecin/chat.read/{patientId}")
    public void markMessagesAsRead(@DestinationVariable Long patientId,
                                   @Payload ReadStatus readStatus) {

        messagerieService.markMessagesAsRead(readStatus.getUserId(), patientId);

        // Notify patient that messages were read
        messagingTemplate.convertAndSend(
                "/user/" + patientId + "/queue/read",
                readStatus
        );
    }

    // REST API: Get unread messages count for doctor
    @GetMapping("/unread-count")
    @ResponseBody
    public Map<String, Object> getUnreadCount(HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (medecin == null) {
            response.put("count", 0);
            return response;
        }

        // Get unread messages count for doctor
        List<Messagerie> unreadMessages = messagerieService.getUnreadMessagesForDoctor(medecin.getId());
        response.put("count", unreadMessages.size());
        response.put("medecinId", medecin.getId());

        return response;
    }

    // REST API: Search patients
    @GetMapping("/search")
    @ResponseBody
    public List<Patient> searchPatients(@RequestParam String query, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return Collections.emptyList();
        }

        List<Patient> doctorPatients = getDoctorPatients(medecin.getId());
        List<Patient> results = new ArrayList<>();

        for (Patient patient : doctorPatients) {
            if (patient.getName().toLowerCase().contains(query.toLowerCase()) ||
                    patient.getEmail().toLowerCase().contains(query.toLowerCase())) {
                results.add(patient);
            }
        }

        return results;
    }

    // REST API: Get chat history with patient
    @GetMapping("/history/{patientId}")
    @ResponseBody
    public List<Messagerie> getChatHistory(@PathVariable Long patientId, HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return Collections.emptyList();
        }

        return messagerieService.getMessagesBetween(patientId, medecin.getId());
    }

    // REST API: Get recent conversations
    @GetMapping("/conversations")
    @ResponseBody
    public List<Map<String, Object>> getRecentConversations(HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return Collections.emptyList();
        }

        // Get doctor's patients
        List<Patient> patients = getDoctorPatients(medecin.getId());
        List<Map<String, Object>> conversations = new ArrayList<>();

        for (Patient patient : patients) {
            // Get last message
            List<Messagerie> messages = messagerieService.getMessagesBetween(patient.getId(), medecin.getId());
            if (!messages.isEmpty()) {
                Messagerie lastMessage = messages.get(messages.size() - 1);

                Map<String, Object> conversation = new HashMap<>();
                conversation.put("patientId", patient.getId());
                conversation.put("patientName", patient.getName());
                conversation.put("lastMessage", lastMessage.getContent());
                conversation.put("lastMessageTime", lastMessage.getDateTime());
                conversation.put("unreadCount", getUnreadCountForPatient(medecin.getId(), patient.getId()));
                conversation.put("isOnline", onlineUsers.containsKey(patient.getId()));

                conversations.add(conversation);
            }
        }

        return conversations;
    }

    // Helper method to get doctor's patients
    private List<Patient> getDoctorPatients(Long doctorId) {
        // You need to implement this based on your business logic
        // This could be patients who have appointments with this doctor
        // or patients assigned to this doctor
        return patientService.getAllPatients(); // Temporary - adjust as needed
    }

    // Helper method to get unread count for specific patient
    private int getUnreadCountForPatient(Long doctorId, Long patientId) {
        List<Messagerie> messages = messagerieService.getMessagesBetween(patientId, doctorId);
        return (int) messages.stream()
                .filter(m -> "PATIENT".equals(m.getSenderType()) && !m.isRead())
                .count();
    }

    // Helper method to broadcast user status
    private void broadcastUserStatus(Long userId, boolean isOnline) {
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("userId", userId);
        statusUpdate.put("isOnline", isOnline);
        statusUpdate.put("timestamp", LocalDateTime.now().toString());
        statusUpdate.put("userType", "MEDECIN");

        messagingTemplate.convertAndSend("/topic/user.status", statusUpdate);
    }
}