package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Consultation;
import com.mycompany.platforme_telemedcine.Models.Ordonance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OrdonanceRepository extends JpaRepository<Ordonance, Long> {
    List<Ordonance> findByDateCreation(Date dateCreation);
    Ordonance findByConsultation(Consultation consultation);




}