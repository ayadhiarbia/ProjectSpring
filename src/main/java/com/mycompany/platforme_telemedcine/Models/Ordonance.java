package com.mycompany.platforme_telemedcine.Models;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Date;
import java.util.List;

@Entity
public class Ordonance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // Title of the prescription
    private String instructions; // Instructions for the patient

    @ElementCollection
    private List<String> medicaments;

    private Date dateCreation;
    private Date dateExpiration; // Expiration date for the prescription
    private Boolean valideeParIA;

    @JsonIgnore
    @OneToOne
    private Consultation consultation;

    // Additional fields for prescription management
    @Column(length = 1000)
    private String notes; // Additional notes from the doctor

    private String status = "active"; // active, expired, completed

    @ManyToOne
    @JoinColumn(name = "patient_id")
    @JsonIgnore
    private Patient patient; // Direct reference to patient

    @ManyToOne
    @JoinColumn(name = "medecin_id")
    @JsonIgnore
    private Medecin medecin; // Doctor who created the prescription

    // Constructors
    public Ordonance() {
        this.dateCreation = new Date();
        this.valideeParIA = false;
        this.status = "active";
    }

    public Ordonance(String title, List<String> medicaments, String instructions) {
        this();
        this.title = title;
        this.medicaments = medicaments;
        this.instructions = instructions;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<String> getMedicaments() {
        return medicaments;
    }

    public void setMedicaments(List<String> medicaments) {
        this.medicaments = medicaments;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Date getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(Date dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public Boolean getValideeParIA() {
        return valideeParIA;
    }

    public void setValideeParIA(Boolean valideeParIA) {
        this.valideeParIA = valideeParIA;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Medecin getMedecin() {
        return medecin;
    }

    public void setMedecin(Medecin medecin) {
        this.medecin = medecin;
    }

    // Helper methods
    public boolean isExpired() {
        if (dateExpiration == null) {
            return false;
        }
        return new Date().after(dateExpiration);
    }

    public boolean isActive() {
        return "active".equals(status) && !isExpired();
    }

    public void addMedicament(String medicament) {
        this.medicaments.add(medicament);
    }

    public void removeMedicament(String medicament) {
        this.medicaments.remove(medicament);
    }

    @Override
    public String toString() {
        return "Ordonance{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", medicaments=" + medicaments +
                ", dateCreation=" + dateCreation +
                ", status='" + status + '\'' +
                '}';
    }
}