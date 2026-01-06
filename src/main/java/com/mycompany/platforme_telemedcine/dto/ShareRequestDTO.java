package com.mycompany.platforme_telemedcine.dto;

import java.util.Date;
import java.util.List;

public class ShareRequestDTO {
    private List<Long> documentIds;
    private List<Long> consultationIds;
    private List<Long> prescriptionIds;
    private String doctorEmail;
    private Date expiresAt;

    // Getters and setters
    public List<Long> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<Long> documentIds) { this.documentIds = documentIds; }
    public List<Long> getConsultationIds() { return consultationIds; }
    public void setConsultationIds(List<Long> consultationIds) { this.consultationIds = consultationIds; }
    public List<Long> getPrescriptionIds() { return prescriptionIds; }
    public void setPrescriptionIds(List<Long> prescriptionIds) { this.prescriptionIds = prescriptionIds; }
    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
}