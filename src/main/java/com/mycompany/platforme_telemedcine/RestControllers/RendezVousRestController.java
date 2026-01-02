package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Controllers.NotificationController;
import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.ConsultationService;
import com.mycompany.platforme_telemedcine.Services.MedecinService;
import com.mycompany.platforme_telemedcine.Services.PatientService;
import com.mycompany.platforme_telemedcine.Services.RendezVousService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rendezvous")
public class RendezVousRestController {

    @Autowired
    private RendezVousService rendezVousService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private MedecinService medecinService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private NotificationController notificationController;


    @PostMapping("/add/{patientId}/{medecinId}")
    public ResponseEntity<RendezVous> addRendezVous(
            @RequestBody RendezVous rendezVous,
            @PathVariable Long patientId,
            @PathVariable Long medecinId) {

        if (rendezVous.getDate() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Patient patient = patientService.getPatientById(patientId);
            Medecin medecin = medecinService.getMedecinById(medecinId);

            rendezVous.setPatient(patient);
            rendezVous.setMedecin(medecin);

            RendezVous saved = rendezVousService.createRendezvous(rendezVous);

            String message = "Nouveau rendez-vous demandé par " + patient.getName();
            System.out.println("Rendez-vous créé - Patient: " + patient.getName() + " (ID: " + patientId + ")");
            System.out.println("Médecin ID: " + medecinId);
            System.out.println("Envoi de notification...");
            notificationController.sendNotificationToMedecin(medecinId, message);

            return new ResponseEntity<>(saved, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/add2/{patientId}/{medecinId}/{consultationId}")
    public ResponseEntity<RendezVous> addRendezVousWithConsultation(
            @RequestBody RendezVous rendezVous,
            @PathVariable Long consultationId,
            @PathVariable Long patientId,
            @PathVariable Long medecinId) {

        try {
            Patient patient = patientService.getPatientById(patientId);
            Medecin medecin = medecinService.getMedecinById(medecinId);
            Consultation consultation = consultationService.getConsultationById(consultationId);

            rendezVous.setConsultation(consultation);
            rendezVous.setPatient(patient);
            rendezVous.setMedecin(medecin);

            RendezVous saved = rendezVousService.createRendezvous(rendezVous);

            String message = "Nouveau rendez-vous avec consultation demandé par " + patient.getName();
            notificationController.sendNotificationToMedecin(medecinId, message);

            return new ResponseEntity<>(saved, HttpStatus.CREATED);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }






    @GetMapping
    public ResponseEntity<List<RendezVous>> getAllRendezVous() {
        List<RendezVous> rendezVousList = rendezVousService.getAllRendezVous();
        if (rendezVousList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(rendezVousList, HttpStatus.OK);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<RendezVous>> getByPatient(@PathVariable Long patientId) {
        List<RendezVous> list = rendezVousService.getByPatient(patientId);
        return list.isEmpty()
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(list, HttpStatus.OK);
    }


    @GetMapping("/{id}")
    public ResponseEntity<RendezVous> getRendezVousById(@PathVariable Long id) {
        RendezVous rendezVous = rendezVousService.getRendezVousById(id);
        if (rendezVous != null) {
            return new ResponseEntity<>(rendezVous, HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/medecin/{medecinId}")
    public ResponseEntity<List<RendezVous>> getRDVsByMedecin(@PathVariable Long medecinId) {
        try {
            System.out.println("Fetching RDVs for medecin ID: " + medecinId);
            List<RendezVous> rdvs = rendezVousService.getRendezVousByMedecinId(medecinId);
            System.out.println("Found " + rdvs.size() + " RDVs for medecin " + medecinId);

            // Log each RDV for debugging
            for (RendezVous rdv : rdvs) {
                System.out.println("RDV ID: " + rdv.getId() +
                        ", Patient: " + (rdv.getPatient() != null ? rdv.getPatient().getName() : "null") +
                        ", Date: " + rdv.getDate() +
                        ", Status: " + rdv.getStatus());
            }

            return ResponseEntity.ok(rdvs);
        } catch (Exception e) {
            System.err.println("Error fetching RDVs for medecin " + medecinId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRendezVous(
            @PathVariable Long id,
            @RequestBody RendezVous updatedData
    ) {
        try {
            RendezVous rdv = rendezVousService.getRendezVousById(id);

            if (rdv == null) {
                return new ResponseEntity<>("Rendez-vous not found", HttpStatus.NOT_FOUND);
            }

            // Update fields
            if (updatedData.getDate() != null) rdv.setDate(updatedData.getDate());
            if (updatedData.getTime() != null) rdv.setTime(updatedData.getTime());
            if (updatedData.getDescription() != null) rdv.setDescription(updatedData.getDescription());

            RendezVous saved = rendezVousService.createRendezvous(rdv);

            return new ResponseEntity<>(saved, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error updating RDV", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // In RendezVousRestController.java
    @PatchMapping("/{id}/status")
    public ResponseEntity<RendezVous> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        try {
            String statusString = body.get("status");
            StatusRendezVous newStatus = StatusRendezVous.valueOf(statusString.toUpperCase());

            RendezVous rdv = rendezVousService.getRendezVousById(id);
            rdv.setStatus(newStatus);

            RendezVous updated = rendezVousService.createRendezvous(rdv);

            if (newStatus == StatusRendezVous.APPROVED) {
                try {
                    Long medecinId = rdv.getMedecin().getId();
                    Long patientId = rdv.getPatient().getId();
                    medecinService.addPatientToMedecin(medecinId, patientId);

                    System.out.println("Patient " + patientId + " added to medecin " + medecinId);
                } catch (Exception e) {
                    System.err.println("Could not add patient to medecin list: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteRendezVous(@PathVariable Long id) {
        try{
            rendezVousService.deleteRendezVous(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
