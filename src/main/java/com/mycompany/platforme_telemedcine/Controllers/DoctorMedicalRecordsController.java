package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/medecin/medical-records")
public class DoctorMedicalRecordsController {

    private final DossierMedicalService dossierMedicalService;
    private final OrdonanceService ordonanceService;
    private final RendezVousService rendezVousService;
    private final PatientService patientService;
    private final ConsultationService consultationService;

    @Autowired
    public DoctorMedicalRecordsController(DossierMedicalService dossierMedicalService,
                                          OrdonanceService ordonanceService,
                                          RendezVousService rendezVousService,
                                          PatientService patientService,
                                          ConsultationService consultationService) {
        this.dossierMedicalService = dossierMedicalService;
        this.ordonanceService = ordonanceService;
        this.rendezVousService = rendezVousService;
        this.patientService = patientService;
        this.consultationService = consultationService;
    }

    // GET: Medecin's medical records dashboard - shows all patients
    @GetMapping
    public String getMedicalRecordsDashboard(HttpSession session, Model model) {
        // First, get the user object without casting
        Object userObj = session.getAttribute("user");

        // Check if the user is actually a Medecin
        if (!(userObj instanceof Medecin)) {
            // If it's an Administrateur, redirect to admin dashboard
            if (userObj instanceof Administrateur) {
                return "redirect:/admin/dashboard";
            }
            // If it's a Patient, redirect to patient dashboard
            else if (userObj instanceof Patient) {
                return "redirect:/patient/dashboard";
            }
            // If it's null or some other type, redirect to login
            else {
                return "redirect:/login";
            }
        }

        // Now we know it's a Medecin, safe to cast
        Medecin medecin = (Medecin) userObj;

        // Get all patients who have appointments with this medecin
        List<Patient> patients = getPatientsWithAppointments(medecin.getId());

        // Get recent patient uploads
        List<DossierMedical> recentPatientUploads = new ArrayList<>();
        for (Patient patient : patients) {
            List<DossierMedical> patientDossiers = dossierMedicalService.getDossiersByPatientId(patient.getId());
            List<DossierMedical> patientUploads = patientDossiers.stream()
                    .filter(doc -> doc.getDescription() != null &&
                            doc.getDescription().contains("=== PATIENT UPLOAD ==="))
                    .sorted((a, b) -> b.getUploadDate().compareTo(a.getUploadDate()))
                    .limit(2)
                    .toList();
            recentPatientUploads.addAll(patientUploads);
        }

        // Sort by upload date
        recentPatientUploads = recentPatientUploads.stream()
                .sorted((a, b) -> b.getUploadDate().compareTo(a.getUploadDate()))
                .limit(10)
                .toList();

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("patients", patients);
        model.addAttribute("recentPatientUploads", recentPatientUploads);
        model.addAttribute("totalPatients", patients.size());
        model.addAttribute("today", LocalDate.now());

        return "medecin/medical-records";
    }

    // GET: View specific patient's medical file
    @GetMapping("/patient/{patientId}")
    public String viewPatientMedicalFile(@PathVariable Long patientId,
                                         HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        // Check if medecin has access to this patient
        if (!hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            return "redirect:/medecin/medical-records?error=patientNotFound";
        }

        // Get all medical dossiers for this patient
        List<DossierMedical> allDocuments = dossierMedicalService.getDossiersByPatientId(patientId);

        // Separate patient uploads from lab results
        List<DossierMedical> patientUploads = allDocuments.stream()
                .filter(doc -> doc.getDescription() != null &&
                        doc.getDescription().contains("=== PATIENT UPLOAD ==="))
                .sorted((a, b) -> b.getUploadDate().compareTo(a.getUploadDate()))
                .collect(Collectors.toList());

        List<DossierMedical> labResults = allDocuments.stream()
                .filter(doc -> doc.getDescription() != null &&
                        doc.getDescription().contains("=== RÉSULTATS DE LABORATOIRE ==="))
                .sorted((a, b) -> b.getUploadDate().compareTo(a.getUploadDate()))
                .collect(Collectors.toList());

        // Get prescriptions written by this medecin for this patient
        List<Ordonance> prescriptions = getPrescriptionsForPatientByMedecin(medecin.getId(), patientId);

        // Get appointments
        List<RendezVous> appointments = getAppointmentsForPatientByMedecin(patientId, medecin.getId());

        // Get consultations
        List<Consultation> consultations = getConsultationsForPatientByMedecin(medecin.getId(), patientId);

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("patient", patient);
        model.addAttribute("patientUploads", patientUploads);
        model.addAttribute("labResults", labResults);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("appointments", appointments);
        model.addAttribute("consultations", consultations);
        model.addAttribute("canUploadLabResults", true);
        model.addAttribute("canWritePrescription", true);
        model.addAttribute("today", LocalDate.now());

        return "medecin/patient-medical-file";
    }

    // GET: Form to upload lab results for patient
    @GetMapping("/patient/{patientId}/upload-lab-results")
    public String uploadLabResultsForm(@PathVariable Long patientId,
                                       HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        if (!hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            return "redirect:/medecin/medical-records?error=patientNotFound";
        }

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("patient", patient);
        model.addAttribute("today", LocalDate.now());

        return "medecin/upload-lab-results";
    }

    // POST: Medecin uploads lab results for patient
    @PostMapping("/patient/{patientId}/upload-lab-results")
    public String uploadLabResults(@PathVariable Long patientId,
                                   @RequestParam("labFile") MultipartFile file,
                                   @RequestParam String testType,
                                   @RequestParam String testDate,
                                   @RequestParam String labName,
                                   @RequestParam String resultsSummary,
                                   @RequestParam(required = false) String notes,
                                   HttpSession session) throws IOException {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        if (!hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            return "redirect:/medecin/medical-records?error=patientNotFound";
        }

        if (file.isEmpty()) {
            return "redirect:/medecin/medical-records/patient/" + patientId + "?error=fileEmpty";
        }

        // Validate file type
        if (!isValidFileType(file.getOriginalFilename())) {
            return "redirect:/medecin/medical-records/patient/" + patientId + "?error=invalidFileType";
        }

        // Create title and description for lab results
        String title = "Résultats de Laboratoire: " + testType + " - " + testDate;
        String description = "=== RÉSULTATS DE LABORATOIRE ===\n\n" +
                "Type de test: " + testType + "\n" +
                "Date du test: " + testDate + "\n" +
                "Laboratoire: " + labName + "\n" +
                "Résumé des résultats: " + resultsSummary + "\n\n" +
                "Notes médicales: " + (notes != null ? notes : "Aucune note particulière") + "\n\n" +
                "=== INFORMATIONS MÉDECIN ===\n" +
                "Médecin traitant: Dr. " + medecin.getName() + "\n" +
                "Date d'upload: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // Upload to dossier medical
        DossierMedical labResultsDossier = dossierMedicalService.uploadDocumentMedical(
                patient, file, title, description);

        return "redirect:/medecin/medical-records/patient/" + patientId + "?success=labResultsUploaded&dossierId=" + labResultsDossier.getId();
    }

    // Add this method to your controller (after the getMedicalRecordsDashboard method)
    @GetMapping("/prescriptions")
    public String getAllPrescriptions(HttpSession session, Model model) {
        // First, get the user object without casting
        Object userObj = session.getAttribute("user");

        // Check if the user is actually a Medecin
        if (!(userObj instanceof Medecin)) {
            if (userObj instanceof Administrateur) {
                return "redirect:/admin/dashboard";
            } else if (userObj instanceof Patient) {
                return "redirect:/patient/dashboard";
            } else {
                return "redirect:/login";
            }
        }

        Medecin medecin = (Medecin) userObj;

        // Get all prescriptions for this doctor
        List<Ordonance> allPrescriptions = ordonanceService.getAllOrdonance()
                .stream()
                .filter(p -> {
                    // Check if prescription is linked to this doctor's consultation
                    if (p.getConsultation() != null &&
                            p.getConsultation().getRendezVous() != null) {
                        return p.getConsultation().getRendezVous().getMedecin().getId().equals(medecin.getId());
                    }
                    return false;
                })
                .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                .collect(Collectors.toList());

        // Count statistics
        Date today = new Date();
        long activePrescriptions = allPrescriptions.stream()
                .filter(p -> p.getDateExpiration() == null || p.getDateExpiration().after(today))
                .count();

        long expiredPrescriptions = allPrescriptions.stream()
                .filter(p -> p.getDateExpiration() != null && p.getDateExpiration().before(today))
                .count();

        // Get expiring soon (within 7 days)
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date sevenDaysFromNow = calendar.getTime();

        List<Ordonance> expiringSoon = allPrescriptions.stream()
                .filter(p -> p.getDateExpiration() != null &&
                        p.getDateExpiration().after(today) &&
                        p.getDateExpiration().before(sevenDaysFromNow))
                .collect(Collectors.toList());

        // ADD THIS: Get patients list for the dropdown
        List<Patient> patients = getPatientsWithAppointments(medecin.getId());

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("prescriptions", allPrescriptions);
        model.addAttribute("activePrescriptions", activePrescriptions);
        model.addAttribute("expiredPrescriptions", expiredPrescriptions);
        model.addAttribute("expiringSoon", expiringSoon);
        model.addAttribute("totalPrescriptions", allPrescriptions.size());
        model.addAttribute("patients", patients); // ADD THIS LINE
        model.addAttribute("today", LocalDate.now());

        return "medecin/prescriptions";
    }
    // GET: Form to create prescription for patient
    @GetMapping("/patient/{patientId}/prescription/create")
    public String createPrescriptionForm(@PathVariable Long patientId,
                                         HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        if (!hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            return "redirect:/medecin/medical-records?error=patientNotFound";
        }

        // Get recent consultation for context
        List<Consultation> recentConsultations = getConsultationsForPatientByMedecin(medecin.getId(), patientId);

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("patient", patient);
        model.addAttribute("consultations", recentConsultations);
        model.addAttribute("today", LocalDate.now());

        return "medecin/create-prescription";
    }

    @PostMapping("/patient/{patientId}/prescription/create")
    public String createPrescription(@PathVariable Long patientId,
                                     @RequestParam String title,
                                     @RequestParam("medicaments") List<String> medicaments,
                                     @RequestParam String instructions,
                                     @RequestParam(required = false) String notes,
                                     @RequestParam(required = false) Long consultationId,
                                     @RequestParam(required = false) String expirationDate,
                                     HttpSession session) {

        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        if (!hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        Patient patient = patientService.getPatientById(patientId);
        if (patient == null) {
            return "redirect:/medecin/medical-records?error=patientNotFound";
        }

        // Create prescription - using the medicaments list directly
        Ordonance prescription = new Ordonance();
        prescription.setTitle(title);
        prescription.setMedicaments(medicaments);
        prescription.setInstructions(instructions);
        prescription.setNotes(notes);
        prescription.setDateCreation(new Date());
        prescription.setPatient(patient);
        prescription.setMedecin(medecin);

        // Set expiration date
        if (expirationDate != null && !expirationDate.isEmpty()) {
            try {
                LocalDate expDate = LocalDate.parse(expirationDate);
                prescription.setDateExpiration(Date.from(expDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            } catch (Exception e) {
                // Log error but continue
                e.printStackTrace();
            }
        }

        // Link to consultation if provided
        if (consultationId != null) {
            Consultation consultation = consultationService.getConsultationById(consultationId);
            if (consultation != null &&
                    consultation.getRendezVous().getMedecin().getId().equals(medecin.getId()) &&
                    consultation.getRendezVous().getPatient().getId().equals(patientId)) {
                prescription.setConsultation(consultation);
            }
        }

        // Save prescription
        Ordonance savedPrescription = ordonanceService.createOrdonance(prescription);
        return "redirect:/medecin/medical-records/prescriptions?success=prescriptionCreated&prescriptionId=" + savedPrescription.getId();
    }


    // GET: Edit prescription form
    @GetMapping("/prescription/{prescriptionId}/edit")
    public String editPrescriptionForm(@PathVariable Long prescriptionId,
                                       HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        Ordonance prescription = ordonanceService.getOrdonanceById(prescriptionId);
        if (prescription == null) {
            return "redirect:/medecin/medical-records?error=prescriptionNotFound";
        }

        // Check if medecin has access to this prescription
        Long patientId = getPatientIdFromPrescription(prescription);
        if (patientId == null || !hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("prescription", prescription);
        model.addAttribute("patientId", patientId);

        return "medecin/edit-prescription";
    }

    // POST: Update prescription
    @PostMapping("/prescription/{prescriptionId}/update")
    public String updatePrescription(@PathVariable Long prescriptionId,
                                     @RequestParam List<String> medicaments,
                                     @RequestParam String instructions,
                                     HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        Ordonance prescription = ordonanceService.getOrdonanceById(prescriptionId);
        if (prescription == null) {
            return "redirect:/medecin/medical-records?error=prescriptionNotFound";
        }

        // Check access
        Long patientId = getPatientIdFromPrescription(prescription);
        if (patientId == null || !hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        prescription.setMedicaments(medicaments);
        ordonanceService.updateOrdonance(prescription);

        return "redirect:/medecin/medical-records/patient/" + patientId + "?success=prescriptionUpdated";
    }

    // POST: Delete prescription
    @PostMapping("/prescription/{prescriptionId}/delete")
    public String deletePrescription(@PathVariable Long prescriptionId,
                                     HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        Ordonance prescription = ordonanceService.getOrdonanceById(prescriptionId);
        if (prescription == null) {
            return "redirect:/medecin/medical-records?error=prescriptionNotFound";
        }

        // Check access
        Long patientId = getPatientIdFromPrescription(prescription);
        if (patientId == null || !hasMedecinPatientRelationship(medecin.getId(), patientId)) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        ordonanceService.deleteOrdonance(prescriptionId);

        return "redirect:/medecin/medical-records/patient/" + patientId + "?success=prescriptionDeleted";
    }

    // GET: View specific dossier/document
    @GetMapping("/document/{dossierId}")
    public String viewDocument(@PathVariable Long dossierId,
                               HttpSession session, Model model) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        DossierMedical document = dossierMedicalService.getDossierMedicalById(dossierId);
        if (document == null) {
            return "redirect:/medecin/medical-records?error=documentNotFound";
        }

        // Check if medecin has access to this patient
        if (!hasMedecinPatientRelationship(medecin.getId(), document.getPatient().getId())) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        model.addAttribute("isPatient", false);
        model.addAttribute("user", medecin);
        model.addAttribute("document", document);
        model.addAttribute("patient", document.getPatient());
        model.addAttribute("isLabResult", isLabResult(document));
        model.addAttribute("isPatientUpload", document.getDescription() != null &&
                document.getDescription().contains("=== PATIENT UPLOAD ==="));

        return "medecin/document-details";
    }

    // POST: Delete lab result (medecin can delete their own uploads)
    @PostMapping("/document/{dossierId}/delete")
    public String deleteDocument(@PathVariable Long dossierId,
                                 HttpSession session) {
        Medecin medecin = (Medecin) session.getAttribute("user");
        if (medecin == null) {
            return "redirect:/login";
        }

        DossierMedical document = dossierMedicalService.getDossierMedicalById(dossierId);
        if (document == null) {
            return "redirect:/medecin/medical-records?error=documentNotFound";
        }

        // Check access
        if (!hasMedecinPatientRelationship(medecin.getId(), document.getPatient().getId())) {
            return "redirect:/medecin/medical-records?error=accessDenied";
        }

        // Only allow deletion of lab results (medecin uploads), not patient uploads
        if (document.getDescription() != null &&
                document.getDescription().contains("=== RÉSULTATS DE LABORATOIRE ===")) {
            Long patientId = document.getPatient().getId();
            dossierMedicalService.deleteDossierMedical(dossierId);
            return "redirect:/medecin/medical-records/patient/" + patientId + "?success=labResultDeleted";
        }

        return "redirect:/medecin/medical-records/patient/" + document.getPatient().getId() + "?error=cannotDeletePatientUpload";
    }

    // Helper Methods
    private List<Patient> getPatientsWithAppointments(Long medecinId) {
        return getRendezVousByMedecin(medecinId)
                .stream()
                .map(RendezVous::getPatient)
                .distinct()
                .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                .collect(Collectors.toList());
    }

    private List<RendezVous> getRendezVousByMedecin(Long medecinId) {
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(medecinId);
        return objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .toList();
    }

    private boolean hasMedecinPatientRelationship(Long medecinId, Long patientId) {
        return getAppointmentsForPatientByMedecin(patientId, medecinId).size() > 0;
    }

    private List<Ordonance> getPrescriptionsForPatientByMedecin(Long medecinId, Long patientId) {
        return ordonanceService.getAllOrdonance()
                .stream()
                .filter(o -> {
                    if (o.getConsultation() != null &&
                            o.getConsultation().getRendezVous() != null) {
                        RendezVous rdv = o.getConsultation().getRendezVous();
                        return rdv.getMedecin().getId().equals(medecinId) &&
                                rdv.getPatient().getId().equals(patientId);
                    }
                    return false;
                })
                .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                .collect(Collectors.toList());
    }

    private List<Consultation> getConsultationsForPatientByMedecin(Long medecinId, Long patientId) {
        return consultationService.getAllConsultations()
                .stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getMedecin().getId().equals(medecinId) &&
                        c.getRendezVous().getPatient().getId().equals(patientId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    private List<RendezVous> getAppointmentsForPatientByMedecin(Long patientId, Long medecinId) {
        return getRendezVousByMedecin(medecinId)
                .stream()
                .filter(rv -> rv.getPatient().getId().equals(patientId))
                .collect(Collectors.toList());
    }

    private Long getPatientIdFromPrescription(Ordonance prescription) {
        if (prescription.getConsultation() != null &&
                prescription.getConsultation().getRendezVous() != null &&
                prescription.getConsultation().getRendezVous().getPatient() != null) {
            return prescription.getConsultation().getRendezVous().getPatient().getId();
        }
        return null;
    }

    private boolean isValidFileType(String filename) {
        if (filename == null) return false;
        String[] allowedExtensions = {".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx", ".txt"};
        String lowerFilename = filename.toLowerCase();
        return Arrays.stream(allowedExtensions)
                .anyMatch(lowerFilename::endsWith);
    }

    private boolean isLabResult(DossierMedical document) {
        if (document == null) return false;
        String desc = document.getDescription() != null ? document.getDescription().toLowerCase() : "";
        return desc.contains("=== résultats de laboratoire ===");
    }
}