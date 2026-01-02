package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Medecin;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedecinRepository extends JpaRepository<Medecin, Long> {
    Medecin findMedecinByEmail(String email);

    // Find doctors by specialty
    List<Medecin> findBySpecialte(String specialte);

    // Search doctors by name, prenom, email, or specialty
    @Query("SELECT m FROM Medecin m WHERE " +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(m.specialte) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Medecin> searchMedecins(@Param("search") String search);

    // Get all unique specialties
    @Query("SELECT DISTINCT m.specialte FROM Medecin m WHERE m.specialte IS NOT NULL")
    List<String> findAllSpecialties();
    List<Medecin> findByStatus(UserStatus status);

    default List<Medecin> findApprovedDoctors() {
        return findByStatus(UserStatus.APPROVED);
    }
}