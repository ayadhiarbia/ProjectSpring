package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.RendezVous;
import com.mycompany.platforme_telemedcine.Models.StatusRendezVous;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    // Derived query methods (Spring Data JPA auto-generates)
    List<RendezVous> findByPatientId(Long patientId);
    List<RendezVous> findByMedecinId(Long medecinId);
    List<RendezVous> findByStatus(StatusRendezVous status);

    // Raw query for debugging / special use
    @Query(value = "SELECT id, date, time, status, description, consultation_id, patient_id, medecin_id " +
            "FROM rendez_vous WHERE patient_id = ?1", nativeQuery = true)
    List<Object[]> findRawAppointmentsByPatientId(Long patientId);

    // Other queries
    @Query("SELECT r FROM RendezVous r WHERE r.date = :date")
    List<RendezVous> findByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(r) FROM RendezVous r WHERE r.date = :date")
    Long countByDate(@Param("date") LocalDate date);

    @Query("SELECT r.status, COUNT(r) FROM RendezVous r GROUP BY r.status")
    List<Object[]> countAppointmentsByStatus();

    @Query("SELECT r FROM RendezVous r WHERE r.date BETWEEN :startDate AND :endDate")
    List<RendezVous> findByDateRange(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
}
