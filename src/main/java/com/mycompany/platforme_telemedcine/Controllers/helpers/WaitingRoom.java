package com.mycompany.platforme_telemedcine.Controllers.helpers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaitingRoom {
    private String roomId;
    private Long consultationId;
    private Map<Long, WaitingParticipant> participants;

    public WaitingRoom(String roomId, Long consultationId) {
        this.roomId = roomId;
        this.consultationId = consultationId;
        this.participants = new HashMap<>();
    }

    public void addParticipant(Long userId, String name, String role) {
        participants.put(userId, new WaitingParticipant(userId, name, role, new Date()));
    }

    public void removeParticipant(Long userId) {
        participants.remove(userId);
    }

    public List<WaitingParticipant> getParticipants() {
        return new ArrayList<>(participants.values());
    }

    public boolean isEmpty() {
        return participants.isEmpty();
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }
}

class WaitingParticipant {
    private Long userId;
    private String name;
    private String role;
    private Date joinedAt;

    public WaitingParticipant(Long userId, String name, String role, Date joinedAt) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    // Getters
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public Date getJoinedAt() { return joinedAt; }
}