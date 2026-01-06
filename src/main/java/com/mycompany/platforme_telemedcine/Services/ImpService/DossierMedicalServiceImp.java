package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.DossierMedical;
import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Repository.DossierMedicalRepository;
import com.mycompany.platforme_telemedcine.Services.DossierMedicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DossierMedicalServiceImp implements DossierMedicalService {

    @Autowired
    private DossierMedicalRepository dossierMedicalRepository;

    @Value("${file.upload-dir:uploads/medical}")
    private String uploadDir;

    @Override
    public DossierMedical save(DossierMedical dossierMedical) {
        return dossierMedicalRepository.save(dossierMedical);
    }

    @Override
    public DossierMedical createDossierMedical(DossierMedical dossierMedical) {
        return dossierMedicalRepository.save(dossierMedical);
    }

    @Override
    public List<DossierMedical> getAllDossierMedical() {
        return dossierMedicalRepository.findAll();
    }

    @Override
    public DossierMedical getDossierMedicalById(Long id) {
        return dossierMedicalRepository.findById(id).orElse(null);
    }

    // Alias method for getDossierMedicalById
    @Override
    public DossierMedical getDossierById(Long dossierId) {
        return dossierMedicalRepository.findById(dossierId).orElse(null);
    }

    @Override
    public void deleteDossierMedical(Long id) {
        DossierMedical dossier = getDossierMedicalById(id);
        if (dossier != null) {
            // Supprimer le fichier physique
            try {
                if (dossier.getFileUrl() != null && dossier.getFileUrl().contains("/")) {
                    String filename = dossier.getFileUrl().substring(dossier.getFileUrl().lastIndexOf("/") + 1);
                    Path filePath = Paths.get(uploadDir).resolve(filename);
                    Files.deleteIfExists(filePath);
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la suppression du fichier: " + e.getMessage());
            }
            dossierMedicalRepository.deleteById(id);
        }
    }

    @Override
    public DossierMedical updateDossierMedical(DossierMedical dossierMedical) {
        return dossierMedicalRepository.save(dossierMedical);
    }

    @Override
    public DossierMedical uploadDocumentMedical(Patient patient, MultipartFile file,
                                                String title, String description) throws IOException {

        // Créer le répertoire s'il n'existe pas
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Générer un nom de fichier sécurisé
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = "doc_" + patient.getId() + "_" + System.currentTimeMillis() + fileExtension;
        Path filePath = uploadPath.resolve(filename);

        // Sauvegarder le fichier
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Créer l'entité DossierMedical
        DossierMedical dossierMedical = new DossierMedical();
        dossierMedical.setPatient(patient);
        dossierMedical.setTitle(title);
        dossierMedical.setDescription(description);
        dossierMedical.setFileName(originalFilename);
        dossierMedical.setFileUrl("/uploads/medical/" + filename); // Update URL format
        dossierMedical.setUploadDate(LocalDateTime.now());

        return dossierMedicalRepository.save(dossierMedical);
    }

    @Override
    public List<DossierMedical> getDossiersByPatientId(Long patientId) {
        return dossierMedicalRepository.findByPatientIdOrderByUploadDateDesc(patientId);
    }

    @Override
    public byte[] getDocumentFile(String filename) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(filename);
        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }
        throw new IOException("Fichier non trouvé: " + filename);
    }

    @Override
    public long getDocumentCountByPatient(Long patientId) {
        return dossierMedicalRepository.countByPatientId(patientId);
    }
}