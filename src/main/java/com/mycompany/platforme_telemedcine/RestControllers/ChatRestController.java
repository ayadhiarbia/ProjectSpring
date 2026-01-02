package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Models.Messagerie;
import com.mycompany.platforme_telemedcine.Services.ImpService.MessagerieServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    @Autowired
    private MessagerieServiceImp messageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> messageData) {
        try {
            System.out.println("📨 REST - Sending message...");

            // Créer un nouvel objet Messagerie
            Messagerie message = new Messagerie();

            // Extraire les données
            if (messageData.get("senderId") != null) {
                message.setSenderId(Long.valueOf(messageData.get("senderId").toString()));
            }
            if (messageData.get("receiverId") != null) {
                message.setReceiverId(Long.valueOf(messageData.get("receiverId").toString()));
            }
            if (messageData.get("senderName") != null) {
                message.setSenderName(messageData.get("senderName").toString());
            }
            if (messageData.get("receiverName") != null) {
                message.setReceiverName(messageData.get("receiverName").toString());
            }
            if (messageData.get("senderRole") != null) {
                message.setSenderRole(messageData.get("senderRole").toString());
            }
            if (messageData.get("content") != null) {
                message.setContent(messageData.get("content").toString());
            }

            // Définir le timestamp
            message.setTimestamp(LocalDateTime.now());

            // Sauvegarder
            Messagerie savedMessage = messageService.saveMessage(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Message envoyé avec succès");
            response.put("data", savedMessage);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ REST Error: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{userId1}/{userId2}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long userId1, @PathVariable Long userId2) {
        try {
            List<Messagerie> messages = messageService.getMessagesBetween(userId1, userId2);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Méthode simple pour tester
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chat API is working!");
    }
}