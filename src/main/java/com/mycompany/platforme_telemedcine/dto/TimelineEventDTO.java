package com.mycompany.platforme_telemedcine.dto;

import java.util.Date;

public class TimelineEventDTO {
    private String type;
    private Date date;
    private String title;
    private String description;

    public TimelineEventDTO(String type, Date date, String title, String description) {
        this.type = type;
        this.date = date;
        this.title = title;
        this.description = description;
    }

    public TimelineEventDTO() {

    }

    // Getters
    public String getType() { return type; }
    public Date getDate() { return date; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public void setEventType(String patientWaiting) {
    }

    public void setConsultationId(Long consultationId) {
    }

    public void setUserId(Long id) {
    }

    public void setDescription(String s) {
    }
}