package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Paiement;
import com.mycompany.platforme_telemedcine.Models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    Paiement findPaiementByDatePaiement(Date datePaiement);
    Paiement findPaiementById(Long id);
    List<Paiement> findByPatient(Patient patient);

    // Find payments by date range
    @Query("SELECT p FROM Paiement p WHERE p.datePaiement BETWEEN :startDate AND :endDate")
    List<Paiement> findByDateRange(@Param("startDate") Date startDate,
                                   @Param("endDate") Date endDate);

    // Sum payments by date range
    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.datePaiement BETWEEN :startDate AND :endDate")
    Double sumByDateRange(@Param("startDate") Date startDate,
                          @Param("endDate") Date endDate);

    // Find payments by status
    List<Paiement> findByStatus(String status);

    // REMOVED: getPaymentStatistics() - since methodePaiement field doesn't exist
}