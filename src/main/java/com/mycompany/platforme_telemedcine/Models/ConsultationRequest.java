package com.mycompany.platforme_telemedcine.Models;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "consultation_requests")
public class ConsultationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Medecin doctor;

    @Enumerated(EnumType.STRING)
    private ConsultationType consultationType;

    @Column(length = 1000)
    private String reason;

    @Column(length = 2000)
    private String symptoms;

    @Enumerated(EnumType.STRING)
    private ConsultationRequestStatus status = ConsultationRequestStatus.PENDING;

    @Temporal(TemporalType.TIMESTAMP)
    private Date preferredDateTime;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    private String rejectionReason;

    @OneToOne(mappedBy = "consultationRequest")
    private Consultation consultation;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private RendezVous appointment;

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Medecin getDoctor() { return doctor; }
    public void setDoctor(Medecin doctor) { this.doctor = doctor; }

    public ConsultationType getConsultationType() { return consultationType; }
    public void setConsultationType(ConsultationType consultationType) { this.consultationType = consultationType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public ConsultationRequestStatus getStatus() { return status; }
    public void setStatus(ConsultationRequestStatus status) { this.status = status; }

    public Date getPreferredDateTime() { return preferredDateTime; }
    public void setPreferredDateTime(Date preferredDateTime) { this.preferredDateTime = preferredDateTime; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Consultation getConsultation() { return consultation; }
    public void setConsultation(Consultation consultation) { this.consultation = consultation; }

    public RendezVous getAppointment() { return appointment; }
    public void setAppointment(RendezVous appointment) { this.appointment = appointment; }
}