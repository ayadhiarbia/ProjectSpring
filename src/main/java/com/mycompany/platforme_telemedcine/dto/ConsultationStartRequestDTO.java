package com.mycompany.platforme_telemedcine.dto;

public class ConsultationStartRequestDTO {
    private Long consultationId;
    private Long doctorId;

    // Getters and setters
    public Long getConsultationId() { return consultationId; }
    public void setConsultationId(Long consultationId) { this.consultationId = consultationId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
}