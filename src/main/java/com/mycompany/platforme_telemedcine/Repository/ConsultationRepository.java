package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Consultation;
import com.mycompany.platforme_telemedcine.Models.ConsultationStatus;
import com.mycompany.platforme_telemedcine.Models.ConsultationType;
import com.mycompany.platforme_telemedcine.Models.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    // Existing methods
    Consultation findConsultationByRendezVous(RendezVous rendezVous);
    List<Consultation> findByDate(Date date);
    List<Consultation> findByDateBetween(Date startDate, Date endDate);
    List<Consultation> findByRendezVousPatientId(Long patientId);
    List<Consultation> findByRendezVousMedecinId(Long medecinId);
    List<Consultation> findByIsActiveTrue();
    List<Consultation> findByRendezVousPatientIdAndIsActiveTrue(Long patientId);
    List<Consultation> findByRendezVousPatientIdAndRendezVousMedecinId(Long patientId, Long medecinId);

    // NEW METHODS FOR CONSULTATION WORKFLOW

    // Find by status
    List<Consultation> findByStatus(ConsultationStatus status);
    List<Consultation> findByRendezVousPatientIdAndStatus(Long patientId, ConsultationStatus status);
    List<Consultation> findByRendezVousMedecinIdAndStatus(Long medecinId, ConsultationStatus status);

    // Find by createdBy (who initiated the consultation)
    List<Consultation> findByCreatedBy(String createdBy);
    List<Consultation> findByRendezVousPatientIdAndCreatedBy(Long patientId, String createdBy);

    // Find by consultation type
    List<Consultation> findByConsultationType(ConsultationType type);

    // Find consultations scheduled for a specific date
    @Query("SELECT c FROM Consultation c WHERE c.scheduledDate >= :startDate AND c.scheduledDate < :endDate")
    List<Consultation> findScheduledConsultationsBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Find upcoming consultations for patient
    @Query("SELECT c FROM Consultation c WHERE c.rendezVous.patient.id = :patientId AND c.status IN :statuses AND c.scheduledDate > :now ORDER BY c.scheduledDate ASC")
    List<Consultation> findUpcomingConsultationsForPatient(@Param("patientId") Long patientId,
                                                           @Param("statuses") List<ConsultationStatus> statuses,
                                                           @Param("now") Date now);

    // Find upcoming consultations for doctor
    @Query("SELECT c FROM Consultation c WHERE c.rendezVous.medecin.id = :medecinId AND c.status IN :statuses AND c.scheduledDate > :now ORDER BY c.scheduledDate ASC")
    List<Consultation> findUpcomingConsultationsForDoctor(@Param("medecinId") Long medecinId,
                                                          @Param("statuses") List<ConsultationStatus> statuses,
                                                          @Param("now") Date now);

    // Find consultations that need approval (patient-initiated and pending)
    @Query("SELECT c FROM Consultation c WHERE c.rendezVous.medecin.id = :medecinId AND c.status = 'PENDING' AND c.createdBy = 'PATIENT'")
    List<Consultation> findPendingPatientConsultationsForDoctor(@Param("medecinId") Long medecinId);

    // Find consultations by date range and status
    @Query("SELECT c FROM Consultation c WHERE c.status = :status AND c.scheduledDate BETWEEN :startDate AND :endDate")
    List<Consultation> findByStatusAndScheduledDateBetween(@Param("status") ConsultationStatus status,
                                                           @Param("startDate") Date startDate,
                                                           @Param("endDate") Date endDate);
}