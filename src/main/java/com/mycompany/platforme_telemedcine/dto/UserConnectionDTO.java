package com.mycompany.platforme_telemedcine.dto;

public class UserConnectionDTO {
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