package com.mycompany.platforme_telemedcine.Controllers.helpers;

import com.mycompany.platforme_telemedcine.Models.ConsultationType;
import com.mycompany.platforme_telemedcine.dto.ChatMessageDTO;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsultationSession {
    private String roomId;
    private Long consultationId;
    private Long patientId;
    private Long doctorId;
    private ConsultationType consultationType;
    private Date startTime;
    private List<ChatMessageDTO> chatHistory;
    private String medicalNotes;

    public ConsultationSession(String roomId, Long consultationId, Long patientId,
                               Long doctorId, ConsultationType consultationType) {
        this.roomId = roomId;
        this.consultationId = consultationId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.consultationType = consultationType;
        this.startTime = new Date();
        this.chatHistory = new ArrayList<>();
    }

    public long getDuration() {
        if (startTime == null) return 0;
        return (System.currentTimeMillis() - startTime.getTime()) / 1000; // in seconds
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public ConsultationType getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationType consultationType) { this.consultationType = consultationType; }

    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }

    public List<ChatMessageDTO> getChatHistory() { return chatHistory; }
    public void setChatHistory(List<ChatMessageDTO> chatHistory) { this.chatHistory = chatHistory; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
}