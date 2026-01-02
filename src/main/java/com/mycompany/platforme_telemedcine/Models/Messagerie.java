package com.mycompany.platforme_telemedcine.Models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messagerie")
public class Messagerie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "sender_role")
    private String senderRole;

    @Lob
    @Column
    private String content;

    private LocalDateTime timestamp;

    @Column(name = "is_read", columnDefinition = "boolean default false")
    private boolean isRead = false;

    // Constructeurs
    public Messagerie() {

    }

    public Messagerie(Long senderId, Long receiverId, String senderName, String receiverName,
                      String senderRole, String content, LocalDateTime timestamp ,boolean isRead) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.receiverName = receiverName;
        this.senderRole = senderRole;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public Messagerie(Long id, Long doctorId, String name, String name1, String patient, String content, LocalDateTime now) {
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}