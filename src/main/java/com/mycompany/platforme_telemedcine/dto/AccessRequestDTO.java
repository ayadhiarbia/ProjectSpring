package com.mycompany.platforme_telemedcine.dto;

import java.util.Date;
import java.util.List;

public class AccessRequestDTO {
    private Long patientId;
    private List<Long> documentIds;
    private String reason;
    private Date expiryDate;
    private List<Long> doctorIds;

    // Getters and setters
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public List<Long> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<Long> documentIds) { this.documentIds = documentIds; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    public List<Long> getDoctorIds() { return doctorIds; }
    public void setDoctorIds(List<Long> doctorIds) { this.doctorIds = doctorIds; }
}