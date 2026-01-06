package com.mycompany.platforme_telemedcine.dto;

import java.util.List;

public class ConsultationEndRequestDTO {
    private String roomId;
    private Long endedBy;
    private String notes;
    private String summary;
    private List<String> prescription;
    private boolean saveAsMedicalRecord;

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getEndedBy() { return endedBy; }
    public void setEndedBy(Long endedBy) { this.endedBy = endedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getPrescription() { return prescription; }
    public void setPrescription(List<String> prescription) { this.prescription = prescription; }

    public boolean isSaveAsMedicalRecord() { return saveAsMedicalRecord; }
    public void setSaveAsMedicalRecord(boolean saveAsMedicalRecord) { this.saveAsMedicalRecord = saveAsMedicalRecord; }
}