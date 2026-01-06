package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.Consultation;
import com.mycompany.platforme_telemedcine.Models.ConsultationStatus;
import com.mycompany.platforme_telemedcine.Models.ConsultationType;

import java.util.Date;
import java.util.List;

public interface ConsultationService {
    // Existing methods
    Consultation createConsultation(Consultation consultation);
    Consultation getConsultationById(Long id);
    Consultation updateConsultation(Consultation consultation);
    void deleteConsultation(Long id);
    List<Consultation> getAllConsultations();
    List<Consultation> getTodayConsultationsForMedecin(Long id);
    List<Consultation> getWaitingConsultationsForMedecin(Long id);
    List<Consultation> getActiveConsultationsForMedecin(Long id);
    List<Consultation> getConsultationsByPatientAndMedecin(Long patientId, Long medecinId);

    // NEW METHODS FOR CONSULTATION WORKFLOW

    // Patient consultation requests - ADD OVERLOADED VERSION
    Consultation createPatientConsultationRequest(Long patientId, Long doctorId,
                                                  ConsultationType type, String reason,
                                                  String symptoms, Date preferredDate);

    Consultation createPatientConsultationRequest(Long patientId, Long doctorId,
                                                  ConsultationType type, String reason,
                                                  String symptoms, Date preferredDate,
                                                  Long rendezVousId);

    // Doctor approval workflow
    Consultation approveConsultation(Long consultationId, Date scheduledDate);
    Consultation rejectConsultation(Long consultationId, String rejectionReason);

    // Consultation management
    Consultation startConsultation(Long consultationId);
    Consultation endConsultation(Long consultationId, String notes);
    Consultation cancelConsultation(Long consultationId, String reason);
    Consultation rescheduleConsultation(Long consultationId, Date newDate);

    // Status-based queries
    List<Consultation> getConsultationsByStatus(ConsultationStatus status);
    List<Consultation> getConsultationsByPatientIdAndStatus(Long patientId, ConsultationStatus status);
    List<Consultation> getConsultationsByDoctorIdAndStatus(Long doctorId, ConsultationStatus status);

    // Dashboard queries
    List<Consultation> getUpcomingConsultationsForPatient(Long patientId);
    List<Consultation> getUpcomingConsultationsForDoctor(Long doctorId);
    List<Consultation> getPendingConsultationsForDoctor(Long doctorId);
    List<Consultation> getCompletedConsultationsForPatient(Long patientId);
    List<Consultation> getCompletedConsultationsForDoctor(Long doctorId);

    // Consultation historypackage com.mycompany.platforme_telemedcine.Services;
    //
    //import com.mycompany.platforme_telemedcine.Models.Consultation;
    //import com.mycompany.platforme_telemedcine.Models.ConsultationStatus;
    //import com.mycompany.platforme_telemedcine.Models.ConsultationType;
    //
    //import java.util.Date;
    //import java.util.List;
    //
    //public interface ConsultationService {
    //    // Existing methods
    //    Consultation createConsultation(Consultation consultation);
    //    Consultation getConsultationById(Long id);
    //    Consultation updateConsultation(Consultation consultation);
    //    void deleteConsultation(Long id);
    //    List<Consultation> getAllConsultations();
    //    List<Consultation> getTodayConsultationsForMedecin(Long id);
    //    List<Consultation> getWaitingConsultationsForMedecin(Long id);
    //    List<Consultation> getActiveConsultationsForMedecin(Long id);
    //    List<Consultation> getConsultationsByPatientAndMedecin(Long patientId, Long medecinId);
    //
    //    // NEW METHODS FOR CONSULTATION WORKFLOW
    //
    //    // Patient consultation requests - OVERLOADED VERSION
    //    Consultation createPatientConsultationRequest(Long patientId, Long doctorId,
    //                                                  ConsultationType type, String reason,
    //                                                  String symptoms, Date preferredDate);
    //
    //    Consultation createPatientConsultationRequest(Long patientId, Long doctorId,
    //                                                  ConsultationType type, String reason,
    //                                                  String symptoms, Date preferredDate,
    //                                                  Long rendezVousId);
    //
    //    // Doctor approval workflow
    //    Consultation approveConsultation(Long consultationId, Date scheduledDate);
    //    Consultation rejectConsultation(Long consultationId, String rejectionReason);
    //
    //    // Consultation management
    //    Consultation startConsultation(Long consultationId);
    //    Consultation endConsultation(Long consultationId, String notes);
    //    Consultation cancelConsultation(Long consultationId, String reason);
    //    Consultation rescheduleConsultation(Long consultationId, Date newDate);
    //
    //    // Status-based queries
    //    List<Consultation> getConsultationsByStatus(ConsultationStatus status);
    //    List<Consultation> getConsultationsByPatientIdAndStatus(Long patientId, ConsultationStatus status);
    //    List<Consultation> getConsultationsByDoctorIdAndStatus(Long doctorId, ConsultationStatus status);
    //
    //    // Dashboard queries
    //    List<Consultation> getUpcomingConsultationsForPatient(Long patientId);
    //    List<Consultation> getUpcomingConsultationsForDoctor(Long doctorId);
    //    List<Consultation> getPendingConsultationsForDoctor(Long doctorId);
    //    List<Consultation> getCompletedConsultationsForPatient(Long patientId);
    //    List<Consultation> getCompletedConsultationsForDoctor(Long doctorId);
    //
    //    // Consultation history
    //    List<Consultation> getConsultationHistoryForPatient(Long patientId);
    //    List<Consultation> getConsultationHistoryForDoctor(Long doctorId);
    //
    //    // Helper methods
    //    Consultation getConsultationByRendezVousId(Long rendezVousId);
    //    boolean canStartConsultation(Long consultationId);
    //    boolean canCancelConsultation(Long consultationId);
    //    boolean canRescheduleConsultation(Long consultationId);
    //
    //    // Statistics
    //    long countConsultationsByPatientAndStatus(Long patientId, ConsultationStatus status);
    //    long countConsultationsByDoctorAndStatus(Long doctorId, ConsultationStatus status);
    //}
    List<Consultation> getConsultationHistoryForPatient(Long patientId);
    List<Consultation> getConsultationHistoryForDoctor(Long doctorId);

    // Helper methods
    Consultation getConsultationByRendezVousId(Long rendezVousId);
    boolean canStartConsultation(Long consultationId);
    boolean canCancelConsultation(Long consultationId);
    boolean canRescheduleConsultation(Long consultationId);

    // Statistics
    long countConsultationsByPatientAndStatus(Long patientId, ConsultationStatus status);
    long countConsultationsByDoctorAndStatus(Long doctorId, ConsultationStatus status);
}