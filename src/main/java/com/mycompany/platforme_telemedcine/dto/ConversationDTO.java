package com.mycompany.platforme_telemedcine.dto;

import java.time.LocalDateTime;

public class ConversationDTO {
    private Long patientId;
    private String patientName;
    private String patientPrenom;
    private String patientEmail;
    private String lastMessageContent;
    private LocalDateTime lastMessageTimestamp;
    private String lastMessageSenderRole;
    private int unreadCount;
    private boolean isOnline;

    // Constructors
    public ConversationDTO() {}

    public ConversationDTO(Long patientId, String patientName, String patientPrenom,
                           String patientEmail, String lastMessageContent,
                           LocalDateTime lastMessageTimestamp, String lastMessageSenderRole,
                           int unreadCount, boolean isOnline) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPrenom = patientPrenom;
        this.patientEmail = patientEmail;
        this.lastMessageContent = lastMessageContent;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderRole = lastMessageSenderRole;
        this.unreadCount = unreadCount;
        this.isOnline = isOnline;
    }

    // Getters and Setters
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