package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.Ordonance;
import java.util.List;

public interface OrdonanceService {
    Ordonance createOrdonance(Ordonance ord);

    // Method for MedicalRecordsController (patient side)
    Ordonance findOrdonanceById(Long id);

    // Method for DoctorMedicalRecordsController (doctor side)
    Ordonance getOrdonanceById(Long id);

    Ordonance updateOrdonance(Ordonance ord);
    void deleteOrdonance(Long id);
    List<Ordonance> getAllOrdonance();
}