package com.mycompany.platforme_telemedcine.Models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "consultation")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date date;
    private String notes;

    @Column(name = "videourl")
    private String videoURL;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultation_type")
    private ConsultationType consultationType;

    @Column(name = "call_room_id")
    private String callRoomId;

    @Column(name = "is_active")
    private Boolean isActive = false;

    // ADD THESE FIELDS FOR APPROVAL WORKFLOW
    @Enumerated(EnumType.STRING)
    private ConsultationStatus status = ConsultationStatus.PENDING;

    @Column(name = "created_by")
    private String createdBy; // "PATIENT" or "DOCTOR"

    @Column(name = "reason", length = 1000)
    private String reason; // Reason for consultation request

    @Column(name = "symptoms", length = 1000) // ADD THIS FIELD
    private String symptoms; // Patient symptoms

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "requested_date")
    private Date requestedDate; // When patient requested

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "approved_date")
    private Date approvedDate; // When doctor approved

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "scheduled_date")
    private Date scheduledDate; // Actual scheduled date

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "preferred_date_time") // ADD THIS FIELD
    private Date preferredDateTime; // Patient's preferred date/time

    @Column(name = "rejection_reason")
    private String rejectionReason; // If rejected

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_time")
    private Date startTime; // When consultation actually started

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_time")
    private Date endTime; // When consultation ended

    @Column(name = "recording_url")
    private String recordingUrl; // Recording of consultation

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // Duration in minutes

    @OneToOne
    @JoinColumn(name = "consultation_request_id")
    private ConsultationRequest consultationRequest; // Link to original request if patient-initiated

    @JsonIgnore
    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "rendez_vous_id")
    private RendezVous rendezVous;

    @JsonIgnore
    @OneToOne(mappedBy = "consultation", cascade = CascadeType.ALL)
    private Ordonance ordonance;

    // Timestamps
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt;

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        if (date == null) {
            date = new Date();
        }
        if (status == null) {
            status = ConsultationStatus.PENDING;
        }
        if (isActive == null) {
            isActive = false;
        }
        if (createdBy == null) {
            createdBy = "PATIENT";
        }
        if (requestedDate == null) {
            requestedDate = new Date();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // Constructors
    public Consultation() {
        this.date = new Date();
        this.isActive = false;
        this.status = ConsultationStatus.PENDING;
        this.createdBy = "PATIENT";
        this.requestedDate = new Date();
    }

    public Consultation(ConsultationType consultationType) {
        this();
        this.consultationType = consultationType;
    }

    public Consultation(RendezVous rendezVous, ConsultationType consultationType) {
        this();
        this.rendezVous = rendezVous;
        this.consultationType = consultationType;
        if (rendezVous != null && rendezVous.getId() != null) {
            this.callRoomId = "room_" + rendezVous.getId() + "_" + System.currentTimeMillis();
        } else {
            this.callRoomId = "room_" + System.currentTimeMillis();
        }
    }

    // NEW: Constructor for consultation from approved appointment
    public Consultation(RendezVous appointment, ConsultationType consultationType,
                        String reason, String symptoms) {
        this();
        this.rendezVous = appointment;  // Use existing appointment
        this.consultationType = consultationType;
        this.reason = reason;
        this.symptoms = symptoms; // Set symptoms directly
        this.notes = "Symptoms: " + symptoms;
        this.createdBy = "PATIENT";
        this.status = ConsultationStatus.PENDING;
        this.requestedDate = new Date();

        // Use appointment date/time
        if (appointment != null && appointment.getDate() != null) {
            this.date = java.sql.Date.valueOf(appointment.getDate());
        }

        if (appointment != null && appointment.getId() != null) {
            this.callRoomId = "room_" + appointment.getId() + "_" + System.currentTimeMillis();
        }
    }

    // Business methods
    public void approve() {
        this.status = ConsultationStatus.APPROVED;
        this.approvedDate = new Date();

        if (rendezVous != null) {
            rendezVous.setStatus(StatusRendezVous.APPROVED);
        }
    }

    public void reject(String reason) {
        this.status = ConsultationStatus.REJECTED;
        this.rejectionReason = reason;
        this.isActive = false;

        if (rendezVous != null) {
            rendezVous.setStatus(StatusRendezVous.CANCELLED);
        }
    }

    public void schedule(Date scheduledDate) {
        this.status = ConsultationStatus.SCHEDULED;
        this.scheduledDate = scheduledDate;
        this.date = scheduledDate; // Also update the main date

        if (rendezVous != null && scheduledDate != null) {
            // You might need to adjust this based on your RendezVous model
            // rendezVous.setDate(convertDateToLocalDate(scheduledDate));
        }
    }

    public void start() {
        this.status = ConsultationStatus.IN_PROGRESS;
        this.startTime = new Date();
        this.isActive = true;

        if (rendezVous != null) {
            rendezVous.setStatus(StatusRendezVous.IN_PROGRESS);
        }
    }

    public void end() {
        this.status = ConsultationStatus.COMPLETED;
        this.endTime = new Date();
        this.isActive = false;

        if (startTime != null && endTime != null) {
            long durationMs = endTime.getTime() - startTime.getTime();
            this.durationMinutes = (int) (durationMs / (1000 * 60));
        }

        if (rendezVous != null) {
            rendezVous.setStatus(StatusRendezVous.COMPLETED);
        }
    }

    public void cancel(String reason) {
        this.status = ConsultationStatus.CANCELLED;
        this.rejectionReason = reason;
        this.isActive = false;

        if (rendezVous != null) {
            rendezVous.setStatus(StatusRendezVous.CANCELLED);
        }
    }

    public void reschedule(Date newDate) {
        this.status = ConsultationStatus.PENDING; // Needs re-approval
        this.scheduledDate = newDate;
        this.requestedDate = new Date(); // Update request date
        this.isActive = false;

        if (rendezVous != null) {
            rendezVous.setStatus(StatusRendezVous.PENDING);
        }
    }

    // Check if consultation can be started
    public boolean canStart() {
        return (status == ConsultationStatus.SCHEDULED ||
                status == ConsultationStatus.APPROVED) &&
                !Boolean.TRUE.equals(isActive);
    }

    // Check if consultation can be cancelled
    public boolean canCancel() {
        return status != ConsultationStatus.COMPLETED &&
                status != ConsultationStatus.CANCELLED &&
                status != ConsultationStatus.REJECTED &&
                !Boolean.TRUE.equals(isActive);
    }

    // Check if consultation can be rescheduled
    public boolean canReschedule() {
        return status != ConsultationStatus.COMPLETED &&
                status != ConsultationStatus.IN_PROGRESS &&
                !Boolean.TRUE.equals(isActive);
    }

    // Helper method to check if consultation is from an appointment
    public boolean isFromAppointment() {
        return rendezVous != null && rendezVous.getId() != null;
    }

    // Helper method to get appointment info
    public String getAppointmentInfo() {
        if (rendezVous != null && rendezVous.getMedecin() != null) {
            return "Dr. " + rendezVous.getMedecin().getName() +
                    " - " + rendezVous.getDate() + " " + rendezVous.getTime();
        }
        return "No appointment linked";
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public void setStatus(ConsultationStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // ADD THIS GETTER AND SETTER
    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public Date getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(Date requestedDate) {
        this.requestedDate = requestedDate;
    }

    public Date getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(Date approvedDate) {
        this.approvedDate = approvedDate;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    // ADD THIS GETTER AND SETTER
    public Date getPreferredDateTime() {
        return preferredDateTime;
    }

    public void setPreferredDateTime(Date preferredDateTime) {
        this.preferredDateTime = preferredDateTime;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public ConsultationRequest getConsultationRequest() {
        return consultationRequest;
    }

    public void setConsultationRequest(ConsultationRequest consultationRequest) {
        this.consultationRequest = consultationRequest;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Consultation{" +
                "id=" + id +
                ", consultationType=" + consultationType +
                ", status=" + status +
                ", createdBy='" + createdBy + '\'' +
                ", reason='" + (reason != null ? reason.substring(0, Math.min(reason.length(), 50)) : "null") + "'" +
                ", symptoms='" + (symptoms != null ? symptoms.substring(0, Math.min(symptoms.length(), 50)) : "null") + "'" +
                ", isActive=" + isActive +
                ", rendezVousId=" + (rendezVous != null ? rendezVous.getId() : "null") +
                '}';
    }
}