package com.mycompany.platforme_telemedcine.util;

import com.mycompany.platforme_telemedcine.Repository.PatientRepository;
import com.mycompany.platforme_telemedcine.Repository.MedecinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    @Autowired private PatientRepository patientRepository;
    @Autowired private MedecinRepository medecinRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- STARTING PASSWORD MIGRATION ---");

        patientRepository.findAll().forEach(patient -> {
            String currentPassword = patient.getPassword();
            // Check if password is NOT already a BCrypt hash
            if (currentPassword != null && !currentPassword.startsWith("$2a$")) {
                System.out.println("Hashing password for patient: " + patient.getEmail());
                patient.setPassword(passwordEncoder.encode(currentPassword));
                patientRepository.save(patient);
            }
        });

        medecinRepository.findAll().forEach(medecin -> {
            String currentPassword = medecin.getPassword();
            if (currentPassword != null && !currentPassword.startsWith("$2a$")) {
                System.out.println("Hashing password for doctor: " + medecin.getEmail());
                medecin.setPassword(passwordEncoder.encode(currentPassword));
                medecinRepository.save(medecin);
            }
        });

        System.out.println("--- PASSWORD MIGRATION COMPLETE ---");
    }
}
