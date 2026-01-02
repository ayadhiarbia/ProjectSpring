package com.mycompany.platforme_telemedcine.DTO;

import com.mycompany.platforme_telemedcine.Models.ConsultationType;
import java.util.Date;

public class ConsultationHistoryDTO {
    private Long id;
    private Date date;
    private ConsultationType consultationType;
    private String doctorName;
    private String notes;
    private Boolean hasPrescription;
    private String duration;

    // Constructors, getters, and setters
    public ConsultationHistoryDTO() {}

    public ConsultationHistoryDTO(Long id, Date date, ConsultationType consultationType,
                                  String doctorName, String notes, Boolean hasPrescription) {
        this.id = id;
        this.date = date;
        this.consultationType = consultationType;
        this.doctorName = doctorName;
        this.notes = notes;
        this.hasPrescription = hasPrescription;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public ConsultationType getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationType consultationType) { this.consultationType = consultationType; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getHasPrescription() { return hasPrescription; }
    public void setHasPrescription(Boolean hasPrescription) { this.hasPrescription = hasPrescription; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
}