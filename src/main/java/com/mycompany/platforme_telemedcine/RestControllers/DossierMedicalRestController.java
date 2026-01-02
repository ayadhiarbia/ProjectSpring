package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Models.DossierMedical;
import com.mycompany.platforme_telemedcine.Models.Patient;
import com.mycompany.platforme_telemedcine.Services.ImpService.DossierMedicalServiceImp;
import com.mycompany.platforme_telemedcine.Services.ImpService.PatientServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dossier-medical")
public class DossierMedicalRestController {

    @Autowired
    private DossierMedicalServiceImp dossierMedicalService;

    @Autowired
    private PatientServiceImp patientService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<DossierMedical>> getDossiersByPatient(@PathVariable Long patientId) {
        try {
            List<DossierMedical> dossiers = dossierMedicalService.getDossiersByPatientId(patientId);
            return ResponseEntity.ok(dossiers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<DossierMedical> getDossierMedical(@PathVariable Long id) {
        try {
            DossierMedical dossier = dossierMedicalService.getDossierMedicalById(id);
            if (dossier != null) {
                return ResponseEntity.ok(dossier);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("patientId") Long patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        try {
            Patient patient = patientService.getPatientById(patientId);
            if (patient == null) {
                return ResponseEntity.badRequest().body("Patient non trouvé");
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Fichier vide");
            }

            DossierMedical dossier = dossierMedicalService.uploadDocumentMedical(
                    patient, file, title, description);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document uploadé avec succès");
            response.put("dossier", dossier);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur d'upload: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }


    @GetMapping("/files/{filename}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String filename) {
        try {
            System.out.println("Download request for file: " + filename);

            byte[] fileContent = dossierMedicalService.getDocumentFile(filename);

            if (fileContent == null || fileContent.length == 0) {
                System.out.println("File not found or empty: " + filename);
                return ResponseEntity.notFound().build();
            }

            System.out.println("File found, size: " + fileContent.length + " bytes");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(fileContent.length);

            return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);

        } catch (IOException e) {
            System.out.println("Error downloading file: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDossierMedical(@PathVariable Long id) {
        try {
            dossierMedicalService.deleteDossierMedical(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document supprimé avec succès");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression: " + e.getMessage());
        }
    }


    @GetMapping("/patient/{patientId}/count")
    public ResponseEntity<Long> getDocumentCount(@PathVariable Long patientId) {
        try {
            long count = dossierMedicalService.getDocumentCountByPatient(patientId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}