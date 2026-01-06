package com.mycompany.platforme_telemedcine.dto;

import java.time.LocalDateTime;

public class ReadStatusDTO {
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