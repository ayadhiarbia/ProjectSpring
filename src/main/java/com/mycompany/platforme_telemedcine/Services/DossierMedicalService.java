package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.DossierMedical;
import com.mycompany.platforme_telemedcine.Models.Patient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DossierMedicalService {
    DossierMedical save(DossierMedical dossierMedical);
    DossierMedical createDossierMedical(DossierMedical dossierMedical);
    List<DossierMedical> getAllDossierMedical();
    DossierMedical getDossierMedicalById(Long id);
    DossierMedical getDossierById(Long dossierId); // Add this method
    void deleteDossierMedical(Long id);
    DossierMedical updateDossierMedical(DossierMedical dossierMedical);

    DossierMedical uploadDocumentMedical(Patient patient, MultipartFile file,
                                         String title, String description) throws IOException;

    List<DossierMedical> getDossiersByPatientId(Long patientId);

    byte[] getDocumentFile(String filename) throws IOException;

    long getDocumentCountByPatient(Long patientId);
}