package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Consultation;
import com.mycompany.platforme_telemedcine.Models.Ordonance;
import com.mycompany.platforme_telemedcine.Models.RendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Consultation findConsultationByRendezVous(RendezVous rendezVous);
    Consultation findConsultationByOrdonance(Ordonance ordonance);
    Consultation findConsultationById(Long id);

    // Find consultations by date
    List<Consultation> findByDate(Date date);

    // Find consultations by date range
    List<Consultation> findByDateBetween(Date startDate, Date endDate);

    // Add this method - it's used in ConsultationServiceImp
    Optional<Consultation> findById(Long id);

    // Add these methods for consultation history
    List<Consultation> findByRendezVousPatientId(Long patientId);
    List<Consultation> findByRendezVousMedecinId(Long medecinId);
    List<Consultation> findByIsActiveTrue();
    List<Consultation> findByRendezVousPatientIdAndIsActiveTrue(Long patientId);
}