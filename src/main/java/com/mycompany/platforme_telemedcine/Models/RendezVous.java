package com.mycompany.platforme_telemedcine.Models;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class RendezVous {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    private String description;
    private String time;

    @Enumerated(EnumType.STRING)
    private StatusRendezVous status = StatusRendezVous.PENDING;

    @ManyToOne
    @JoinColumn(name = "medecin_id")
    Medecin medecin;
    @ManyToOne
    @JoinColumn(name = "patient_id")
    Patient patient;

    @OneToOne
    @JoinColumn(name = "consultation_id")
    Consultation consultation;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Medecin getMedecin() {
        return medecin;
    }

    public void setMedecin(Medecin medecin) {
        this.medecin = medecin;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }

    public StatusRendezVous getStatus() {
        return status;
    }

    public void setStatus(StatusRendezVous status) {
        this.status = status;
    }
}
