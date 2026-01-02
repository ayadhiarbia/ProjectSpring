package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.DossierMedical;
import com.mycompany.platforme_telemedcine.Models.Patient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DossierMedicalService {
    DossierMedical createDossierMedical(DossierMedical dossierMedical);
    List<DossierMedical> getAllDossierMedical();
    DossierMedical getDossierMedicalById(Long id);
    void deleteDossierMedical(Long id);
    DossierMedical updateDossierMedical(DossierMedical dossierMedical);

    DossierMedical uploadDocumentMedical(Patient patient, MultipartFile file, String s, String s1) throws IOException;

    List<DossierMedical> getDossiersByPatientId(Long id);

    byte[] getDocumentFile(String filename) throws IOException;
}