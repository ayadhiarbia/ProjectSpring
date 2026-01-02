package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Models.Messagerie;
import com.mycompany.platforme_telemedcine.Services.ImpService.MessagerieServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessagerieServiceImp messageService;

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload Map<String, Object> messageData) {
        try {
            System.out.println("🔵 WebSocket - Received message data: " + messageData);

            // Créer un objet Messagerie à partir des données reçues
            Messagerie chatMessage = new Messagerie();

            // Extraire les données du Map
            if (messageData.get("senderId") != null) {
                chatMessage.setSenderId(Long.valueOf(messageData.get("senderId").toString()));
            }
            if (messageData.get("receiverId") != null) {
                chatMessage.setReceiverId(Long.valueOf(messageData.get("receiverId").toString()));
            }
            if (messageData.get("senderName") != null) {
                chatMessage.setSenderName(messageData.get("senderName").toString());
            }
            if (messageData.get("receiverName") != null) {
                chatMessage.setReceiverName(messageData.get("receiverName").toString());
            }
            if (messageData.get("senderRole") != null) {
                chatMessage.setSenderRole(messageData.get("senderRole").toString());
            }
            if (messageData.get("content") != null) {
                chatMessage.setContent(messageData.get("content").toString());
            }

            // Définir le timestamp
            chatMessage.setTimestamp(LocalDateTime.now());

            // Sauvegarder dans la base de données
            Messagerie savedMessage = messageService.saveMessage(chatMessage);
            System.out.println("💾 WebSocket - Message saved with ID: " + savedMessage.getId());

            // Préparer la réponse pour le frontend
            Map<String, Object> response = Map.of(
                    "id", savedMessage.getId(),
                    "senderId", savedMessage.getSenderId(),
                    "receiverId", savedMessage.getReceiverId(),
                    "senderName", savedMessage.getSenderName(),
                    "receiverName", savedMessage.getReceiverName(),
                    "senderRole", savedMessage.getSenderRole(),
                    "content", savedMessage.getContent(),
                    "timestamp", savedMessage.getTimestamp().toString()
            );

            // Envoyer aux rooms concernées
            String userRoom = "/topic/chat/" + chatMessage.getSenderId() + "/" + chatMessage.getReceiverId();
            String otherUserRoom = "/topic/chat/" + chatMessage.getReceiverId() + "/" + chatMessage.getSenderId();

            messagingTemplate.convertAndSend(userRoom, response);
            messagingTemplate.convertAndSend(otherUserRoom, response);

            System.out.println("📤 WebSocket - Message broadcasted to rooms: " + userRoom + " and " + otherUserRoom);

        } catch (Exception e) {
            System.err.println("❌ WebSocket Error: " + e.getMessage());
            e.printStackTrace();

            // Envoyer un message d'erreur
            Map<String, Object> errorResponse = Map.of(
                    "error", true,
                    "errorMessage", "Failed to send message: " + e.getMessage()
            );
            messagingTemplate.convertAndSend("/topic/errors", errorResponse);
        }
    }
}