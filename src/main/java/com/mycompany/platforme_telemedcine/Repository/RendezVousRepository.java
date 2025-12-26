package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Medecin;
import com.mycompany.platforme_telemedcine.Models.RendezVous;
import com.mycompany.platforme_telemedcine.Models.StatusRendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {
    RendezVous findRendezVousByDate(Date date);
    RendezVous findRendezVousById(Long id);

    // Find appointments by status
    List<RendezVous> findByStatus(StatusRendezVous status);

    // Find appointments by patient ID
    List<RendezVous> findByPatientId(Long patientId);

    // Find appointments by doctor ID
    List<RendezVous> findByMedecinId(Long medecinId);

    // Find appointments by date
    @Query("SELECT r FROM RendezVous r WHERE r.date = :date")
    List<RendezVous> findByDate(@Param("date") LocalDate date);

    // Count appointments by date
    @Query("SELECT COUNT(r) FROM RendezVous r WHERE r.date = :date")
    Long countByDate(@Param("date") LocalDate date);

    // Get appointment statistics by status
    @Query("SELECT r.status, COUNT(r) FROM RendezVous r GROUP BY r.status")
    List<Object[]> countAppointmentsByStatus();

    // Find appointments by date range
    @Query("SELECT r FROM RendezVous r WHERE r.date BETWEEN :startDate AND :endDate")
    List<RendezVous> findByDateRange(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
}