package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.Patient;

import java.util.List;

public interface PatientService {
    Patient createPatient(Patient patient);
    Patient updatePatient(Patient patient);
    void deletePatientById(Long id);
    List<Patient> getAllPatients();
    Patient getPatientById(Long id);
    Patient getPatientByEmail(String email);

}
