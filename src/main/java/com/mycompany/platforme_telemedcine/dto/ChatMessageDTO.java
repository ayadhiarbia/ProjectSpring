package com.mycompany.platforme_telemedcine.dto;

import java.util.Date;

public class ChatMessageDTO {
    private String roomId;
    private Long consultationId;
    private Long senderId;
    private String senderName;
    private String content;
    private Date timestamp;
    private String messageType; // TEXT, FILE, SYSTEM, WARNING

    public ChatMessageDTO() {
        this.timestamp = new Date();
        this.messageType = "TEXT";
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

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