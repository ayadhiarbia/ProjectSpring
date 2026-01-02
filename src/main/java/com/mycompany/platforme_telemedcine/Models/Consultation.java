package com.mycompany.platforme_telemedcine.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;

@Entity
public class Consultation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date date;
    private String notes;
    private String videoURL;

    // Add these fields
    @Enumerated(EnumType.STRING)
    private ConsultationType consultationType;

    private String callRoomId;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @JsonIgnore
    @OneToOne
    private RendezVous rendezVous;

    @JsonIgnore
    @OneToOne
    private Ordonance ordonance;

    // Constructors
    public Consultation() {
        this.date = new Date();
        this.isActive = false;
    }

    public Consultation(ConsultationType consultationType) {
        this();
        this.consultationType = consultationType;
    }

    public Consultation(RendezVous rendezVous, ConsultationType consultationType) {
        this();
        this.rendezVous = rendezVous;
        this.consultationType = consultationType;
        this.callRoomId = "room_" + rendezVous.getId() + "_" + System.currentTimeMillis();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getVideoURL() {
        return videoURL;
    }

    public void setVideoURL(String videoURL) {
        this.videoURL = videoURL;
    }

    public ConsultationType getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(ConsultationType consultationType) {
        this.consultationType = consultationType;
    }

    public String getCallRoomId() {
        return callRoomId;
    }

    public void setCallRoomId(String callRoomId) {
        this.callRoomId = callRoomId;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public RendezVous getRendezVous() {
        return rendezVous;
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVous = rendezVous;
    }

    public Ordonance getOrdonance() {
        return ordonance;
    }

    public void setOrdonance(Ordonance ordonance) {
        this.ordonance = ordonance;
    }
}