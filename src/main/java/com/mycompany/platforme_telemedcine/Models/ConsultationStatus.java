package com.mycompany.platforme_telemedcine.Models;

public enum ConsultationStatus {
    PENDING,        // Waiting for doctor approval
    APPROVED,       // Doctor approved, but not scheduled
    REJECTED,       // Doctor rejected the request
    SCHEDULED,      // Scheduled for a specific date/time
    IN_PROGRESS,    // Currently happening
    COMPLETED,      // Successfully completed
    CANCELLED,      // Cancelled by patient or doctor
    RESCHEDULED,    // Rescheduled to new time
    NO_SHOW,        // Patient didn't attend
    EXPIRED         // Request expired (not approved within time limit)
}