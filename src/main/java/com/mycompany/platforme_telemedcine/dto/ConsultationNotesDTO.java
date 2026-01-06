package com.mycompany.platforme_telemedcine.dto;

import java.util.Date;

public class ConsultationNotesDTO {
    private String roomId;
    private Long updatedBy;
    private String notes;
    private Date updatedAt;

    public ConsultationNotesDTO() {
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}