package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Medecin;
import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Models.User;
import com.mycompany.platforme_telemedcine.Models.UserStatus;
import com.mycompany.platforme_telemedcine.Repository.MedecinRepository;
import com.mycompany.platforme_telemedcine.Repository.PatientRepository;
import com.mycompany.platforme_telemedcine.Repository.UserRepository;
import com.mycompany.platforme_telemedcine.Services.MedecinService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedecinServiceImp implements MedecinService {

    @Autowired
    private MedecinRepository medecinRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    // ... (keep all your existing methods) ...

    @Override
    public List<Medecin> getApprovedDoctors() {
        // This is the cleanest and safest way
        return medecinRepository.findByStatus(UserStatus.APPROVED);
    }

    @Override
    public List<Medecin> getDoctorsByStatus(String statusStr) {
        try {
            // Convert the String from the UI/Controller into the actual Enum
            UserStatus statusEnum = UserStatus.valueOf(statusStr.toUpperCase());
            return medecinRepository.findByStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            return List.of(); // Return empty list if the status string is invalid
        }
    }
    // Also add this method for filtering by specialty
    @Override
    public List<Medecin> getApprovedDoctorsBySpecialty(String specialty) {
        List<Medecin> allApproved = medecinRepository.findApprovedDoctors();
        return allApproved.stream()
                .filter(d -> d.getSpecialte() != null && d.getSpecialte().equalsIgnoreCase(specialty))
                .toList();
    }



    @Override
    public Medecin createMedecin(Medecin m) {
        User savedUser = userRepository.save(m);
        return medecinRepository.save(m);
    }

    @Override
    public Medecin updateMedecin(Medecin m) {
        return medecinRepository.save(m);
    }

    @Override
    public void deleteMedecinById(Long id) {
        this.medecinRepository.deleteById(id);
    }

    @Override
    public Medecin getMedecinById(Long id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé avec l'ID: " + id));
    }

    @Override
    public List<Medecin> getAllMedecin() {
        return medecinRepository.findAll();
    }

    // ADD THIS METHOD - It's in your interface
    @Override
    public List<Medecin> getAllMedecins() {
        return getAllMedecin(); // Call the existing method
    }

    @Override
    public Medecin getMedecinByEmail(String email) {
        Medecin medecin = medecinRepository.findMedecinByEmail(email);
        if (medecin == null) {
            throw new RuntimeException("Médecin non trouvé avec l'email: " + email);
        }
        return medecin;
    }

    @Override
    @Transactional
    public void addPatientToMedecin(Long medecinId, Long patientId) {
        Medecin medecin = medecinRepository.findById(medecinId)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient non trouvé"));

        medecin.addPatient(patient);
        medecinRepository.save(medecin);
    }

    @Override
    @Transactional
    public void removePatientFromMedecin(Long medecinId, Long patientId) {
        Medecin medecin = medecinRepository.findById(medecinId)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient non trouvé"));

        medecin.removePatient(patient);
        medecinRepository.save(medecin);
    }

    @Override
    public List<Patient> getPatientsByMedecin(Long medecinId) {
        Medecin medecin = medecinRepository.findById(medecinId)
                .orElseThrow(() -> new RuntimeException("Médecin non trouvé"));
        return medecin.getPatients();
    }
}