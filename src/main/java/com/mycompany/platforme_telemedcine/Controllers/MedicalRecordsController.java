package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.dto.*;
import com.mycompany.platforme_telemedcine.Services.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/patient")
public class MedicalRecordsController {

    private final DossierMedicalService dossierMedicalService;
    private final ConsultationService consultationService;
    private final OrdonanceService ordonanceService;
    private final RendezVousService rendezVousService;

    @Autowired
    public MedicalRecordsController(DossierMedicalService dossierMedicalService,
                                    ConsultationService consultationService,
                                    OrdonanceService ordonanceService,
                                    RendezVousService rendezVousService) {
        this.dossierMedicalService = dossierMedicalService;
        this.consultationService = consultationService;
        this.ordonanceService = ordonanceService;
        this.rendezVousService = rendezVousService;
    }

    // GET: Main dashboard for patient's medical records
    @Transactional
    @GetMapping("/medical-records")
    public String getMedicalRecordsDashboard(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        // Get all medical records
        List<DossierMedical> allDocuments = dossierMedicalService.getDossiersByPatientId(patient.getId());
        List<Consultation> consultations = getConsultationsByPatientId(patient.getId());
        List<Ordonance> prescriptions = getPrescriptionsByPatientId(patient.getId());
        List<RendezVous> appointments = rendezVousService.getByPatient(patient.getId());

        // Separate patient uploads from lab results (doctor uploads)
        List<DossierMedical> patientUploads = allDocuments.stream()
                .filter(doc -> doc.getDescription() != null &&
                        doc.getDescription().contains("=== PATIENT UPLOAD ==="))
                .collect(Collectors.toList());

        List<DossierMedical> labResults = allDocuments.stream()
                .filter(doc -> doc.getDescription() != null &&
                        doc.getDescription().contains("=== RÉSULTATS DE LABORATOIRE ==="))
                .collect(Collectors.toList());

        // Group patient uploads by category
        Map<String, List<DossierMedical>> documentsByCategory = groupDocumentsByCategory(patientUploads);

        // Prepare summary
        MedicalSummaryDTO summary = prepareMedicalSummary(patient, consultations, prescriptions, appointments);

        model.addAttribute("isPatient", true);
        model.addAttribute("user", patient);
        model.addAttribute("patientUploads", patientUploads);
        model.addAttribute("labResults", labResults);
        model.addAttribute("documentsByCategory", documentsByCategory);
        model.addAttribute("consultations", consultations);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("appointments", appointments);
        model.addAttribute("summary", summary);
        model.addAttribute("uploadCount", patientUploads.size());
        model.addAttribute("labResultCount", labResults.size());
        model.addAttribute("consultationCount", consultations.size());
        model.addAttribute("prescriptionCount", prescriptions.size());
        model.addAttribute("totalDocuments", allDocuments.size());

        return "patient/medical-records"; // This matches your file structure
    }

    // POST: Patient uploads their own documents
    @PostMapping("/medical-records/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 @RequestParam("title") String title,
                                 @RequestParam("description") String description,
                                 @RequestParam("category") String category,
                                 HttpSession session) throws IOException {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        // Validate file type
        if (!isValidFileType(file.getOriginalFilename())) {
            return "redirect:/patient/medical-records?error=invalidFileType";
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return "redirect:/patient/medical-records?error=fileTooLarge";
        }

        // Create description indicating patient uploaded
        String fullDescription = "=== PATIENT UPLOAD ===\n" +
                "Catégorie: " + category.toUpperCase() + "\n" +
                "Description: " + description + "\n" +
                "Uploadé par: " + patient.getName() + "\n" +
                "Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        DossierMedical document = dossierMedicalService.uploadDocumentMedical(
                patient, file, title, fullDescription);

        return "redirect:/patient/medical-records?success=documentUploaded&documentId=" + document.getId();
    }

    // GET: View specific document
    @GetMapping("/medical-records/document/{id}")
    public String viewDocument(@PathVariable Long id, HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        DossierMedical document = dossierMedicalService.getDossierMedicalById(id);
        if (document == null || !document.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/medical-records?error=documentNotFound";
        }

        model.addAttribute("isPatient", true);
        model.addAttribute("user", patient);
        model.addAttribute("document", document);
        model.addAttribute("fileType", getFileType(document.getFileName()));
        model.addAttribute("isLabResult", isLabResult(document));
        model.addAttribute("isPatientUpload", document.getDescription() != null &&
                document.getDescription().contains("=== PATIENT UPLOAD ==="));

        // This should return a specific document view. Since you don't have document-details.html,
        // redirect to medical-records with the document in context
        return "patient/medical-records"; // Or create a document-details.html if needed
    }

    // GET: Download document
    @GetMapping("/medical-records/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, HttpSession session) throws IOException {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return ResponseEntity.status(401).build();
        }

        DossierMedical document = dossierMedicalService.getDossierMedicalById(id);
        if (document == null || !document.getPatient().getId().equals(patient.getId())) {
            return ResponseEntity.notFound().build();
        }

        String filename = document.getFileUrl().substring(document.getFileUrl().lastIndexOf("/") + 1);
        byte[] fileContent = dossierMedicalService.getDocumentFile(filename);

        ByteArrayResource resource = new ByteArrayResource(fileContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getFileName() + "\"")
                .contentLength(fileContent.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // GET: View lab results (uploaded by doctors)
    @GetMapping("/medical-records/lab-results")
    public String getLabResults(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<DossierMedical> labResults = dossierMedicalService.getDossiersByPatientId(patient.getId())
                .stream()
                .filter(this::isLabResult)
                .sorted((a, b) -> b.getUploadDate().compareTo(a.getUploadDate()))
                .collect(Collectors.toList());

        // Group by test type
        Map<String, List<DossierMedical>> resultsByType = labResults.stream()
                .collect(Collectors.groupingBy(this::extractTestType));

        model.addAttribute("isPatient", true);
        model.addAttribute("user", patient);
        model.addAttribute("labResults", labResults);
        model.addAttribute("resultsByType", resultsByType);
        model.addAttribute("totalResults", labResults.size());

        return "patient/medical-records"; // Display lab results within medical-records page
    }

    // GET: View prescriptions (from doctors)
    @GetMapping("/prescriptions")
    public String getPrescriptions(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<Ordonance> prescriptions = getPrescriptionsByPatientId(patient.getId());

        // Group by status
        Map<String, List<Ordonance>> prescriptionsByStatus = new HashMap<>();
        prescriptionsByStatus.put("active", new ArrayList<>());
        prescriptionsByStatus.put("completed", new ArrayList<>());

        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        for (Ordonance prescription : prescriptions) {
            if (prescription.getDateCreation() == null) {
                prescriptionsByStatus.get("completed").add(prescription);
                continue;
            }

            try {
                LocalDate prescriptionDate = prescription.getDateCreation().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();

                if (prescriptionDate.isAfter(oneMonthAgo) &&
                        !prescriptionDate.isAfter(LocalDate.now().plusMonths(1))) {
                    prescriptionsByStatus.get("active").add(prescription);
                } else {
                    prescriptionsByStatus.get("completed").add(prescription);
                }
            } catch (Exception e) {
                prescriptionsByStatus.get("completed").add(prescription);
            }
        }

        model.addAttribute("isPatient", true);
        model.addAttribute("user", patient);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("prescriptionsByStatus", prescriptionsByStatus);
        model.addAttribute("activeCount", prescriptionsByStatus.get("active").size());
        model.addAttribute("completedCount", prescriptionsByStatus.get("completed").size());
        model.addAttribute("totalCount", prescriptions.size());

        return "patient/prescriptions"; // This matches your file structure
    }

    // GET: View specific prescription
    @GetMapping("/prescriptions/{id}")
    public String viewPrescription(@PathVariable Long id, HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Ordonance prescription = ordonanceService.findOrdonanceById(id);
        if (prescription == null) {
            return "redirect:/patient/prescriptions?error=notFound";
        }

        // Check ownership with null safety
        boolean isOwnedByPatient = false;

        // Method 1: Direct patient link
        if (prescription.getPatient() != null &&
                prescription.getPatient().getId().equals(patient.getId())) {
            isOwnedByPatient = true;
        }
        // Method 2: Through consultation chain
        else if (prescription.getConsultation() != null &&
                prescription.getConsultation().getRendezVous() != null &&
                prescription.getConsultation().getRendezVous().getPatient() != null &&
                prescription.getConsultation().getRendezVous().getPatient().getId().equals(patient.getId())) {
            isOwnedByPatient = true;
        }
        // Method 3: If everything is null, allow access (for testing)
        else if (prescription.getPatient() == null &&
                (prescription.getConsultation() == null ||
                        prescription.getConsultation().getRendezVous() == null ||
                        prescription.getConsultation().getRendezVous().getPatient() == null)) {
            isOwnedByPatient = true;
        }

        if (!isOwnedByPatient) {
            return "redirect:/patient/prescriptions?error=notFound";
        }

        model.addAttribute("isPatient", true);
        model.addAttribute("user", patient);
        model.addAttribute("prescription", prescription);

        // Handle null consultation gracefully
        if (prescription.getConsultation() != null) {
            model.addAttribute("consultation", prescription.getConsultation());
            if (prescription.getConsultation().getRendezVous() != null &&
                    prescription.getConsultation().getRendezVous().getMedecin() != null) {
                model.addAttribute("doctor", prescription.getConsultation().getRendezVous().getMedecin());
            }
        }

        // Also check direct medecin field
        if (prescription.getMedecin() != null) {
            model.addAttribute("doctor", prescription.getMedecin());
        }

        return "patient/prescriptions"; // This matches your file structure
    }

    // GET: Delete patient's uploaded document
    @PostMapping("/medical-records/document/{id}/delete")
    public String deleteDocument(@PathVariable Long id, HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        DossierMedical document = dossierMedicalService.getDossierMedicalById(id);
        if (document == null || !document.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/medical-records?error=documentNotFound";
        }

        // Only allow deletion of patient uploads (not lab results from doctors)
        if (document.getDescription() != null &&
                document.getDescription().contains("=== PATIENT UPLOAD ===")) {
            dossierMedicalService.deleteDossierMedical(id);
            return "redirect:/patient/medical-records?success=documentDeleted";
        }

        return "redirect:/patient/medical-records?error=cannotDeleteLabResults";
    }

    // GET: Export medical summary as PDF
    @GetMapping("/medical-records/export/pdf")
    public ResponseEntity<byte[]> exportMedicalSummary(HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            byte[] pdfContent = generateMedicalSummaryPDF(patient);

            String filename = "dossier_medical_" + patient.getName() + "_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // Helper Methods
    private List<Consultation> getConsultationsByPatientId(Long patientId) {
        return consultationService.getAllConsultations().stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getPatient().getId().equals(patientId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    private List<Ordonance> getPrescriptionsByPatientId(Long patientId) {
        return ordonanceService.getAllOrdonance().stream()
                .filter(o -> {
                    // Method 1: Check direct patient link (new way)
                    if (o.getPatient() != null && o.getPatient().getId().equals(patientId)) {
                        return true;
                    }

                    // Method 2: Check through consultation chain (old way)
                    if (o.getConsultation() != null &&
                            o.getConsultation().getRendezVous() != null &&
                            o.getConsultation().getRendezVous().getPatient() != null &&
                            o.getConsultation().getRendezVous().getPatient().getId().equals(patientId)) {
                        return true;
                    }

                    // Method 3: If both are null, include it anyway (for testing)
                    return o.getPatient() == null && o.getConsultation() == null;
                })
                .sorted((a, b) -> {
                    // Handle null dates safely
                    if (a.getDateCreation() == null && b.getDateCreation() == null) return 0;
                    if (a.getDateCreation() == null) return 1;
                    if (b.getDateCreation() == null) return -1;
                    return b.getDateCreation().compareTo(a.getDateCreation());
                })
                .collect(Collectors.toList());
    }

    private Map<String, List<DossierMedical>> groupDocumentsByCategory(List<DossierMedical> documents) {
        Map<String, List<DossierMedical>> grouped = new HashMap<>();
        grouped.put("Medical Reports", new ArrayList<>());
        grouped.put("Scans/Imaging", new ArrayList<>());
        grouped.put("Prescriptions", new ArrayList<>());
        grouped.put("Vaccinations", new ArrayList<>());
        grouped.put("Other", new ArrayList<>());

        for (DossierMedical doc : documents) {
            String category = detectCategory(doc);
            grouped.get(category).add(doc);
        }

        return grouped;
    }

    private String detectCategory(DossierMedical doc) {
        String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";
        String desc = doc.getDescription() != null ? doc.getDescription().toLowerCase() : "";

        if (title.contains("report") || title.contains("rapport") || title.contains("consultation")) {
            return "Medical Reports";
        } else if (title.contains("scan") || title.contains("x-ray") || title.contains("mri") ||
                title.contains("radiologie")) {
            return "Scans/Imaging";
        } else if (title.contains("prescription") || title.contains("ordonnance")) {
            return "Prescriptions";
        } else if (title.contains("vaccin") || title.contains("vaccination")) {
            return "Vaccinations";
        } else {
            return "Other";
        }
    }

    private MedicalSummaryDTO prepareMedicalSummary(Patient patient,
                                                    List<Consultation> consultations,
                                                    List<Ordonance> prescriptions,
                                                    List<RendezVous> appointments) {
        MedicalSummaryDTO summary = new MedicalSummaryDTO();
        summary.setPatientName(patient.getName());
        summary.setAge(calculateAge(patient.getDataNaissance()));
        summary.setAllergies(extractAllergies(patient.getAntecedentsMedicaux()));
        summary.setChronicConditions(extractChronicConditions(consultations));
        summary.setCurrentMedications(extractCurrentMedications(prescriptions));
        summary.setLastConsultation(getLastConsultationDate(consultations));
        summary.setNextAppointment(getNextAppointmentDate(appointments));

        return summary;
    }

    private byte[] generateMedicalSummaryPDF(Patient patient) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Add title
        document.add(new Paragraph("DOSSIER MÉDICAL")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(20)
                .setBold());

        document.add(new Paragraph(" "));

        // Add patient info
        document.add(new Paragraph("Patient: " + patient.getName())
                .setFontSize(14)
                .setBold());
        document.add(new Paragraph("Date de naissance: " +
                patient.getDataNaissance().toString()));

        document.add(new Paragraph(" "));

        // Add medical history
        document.add(new Paragraph("ANTÉCÉDENTS MÉDICAUX")
                .setFontSize(16)
                .setBold());
        document.add(new Paragraph(patient.getAntecedentsMedicaux() != null ? patient.getAntecedentsMedicaux() : "Aucun"));

        document.add(new Paragraph(" "));

        // Add recent consultations
        List<Consultation> consultations = getConsultationsByPatientId(patient.getId());
        if (!consultations.isEmpty()) {
            document.add(new Paragraph("DERNIÈRES CONSULTATIONS")
                    .setFontSize(16)
                    .setBold());

            for (Consultation consultation : consultations.stream()
                    .filter(c -> c.getDate() != null && c.getRendezVous() != null && c.getRendezVous().getMedecin() != null)
                    .limit(5)
                    .collect(Collectors.toList())) {
                document.add(new Paragraph(
                        consultation.getDate() + " - Dr. " +
                                consultation.getRendezVous().getMedecin().getName() +
                                " - " + consultation.getConsultationType())
                        .setMarginLeft(20));
            }
        }
        document.close();
        return baos.toByteArray();
    }

    private boolean isValidFileType(String filename) {
        if (filename == null) return false;
        String[] allowedExtensions = {".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx", ".txt"};
        String lowerFilename = filename.toLowerCase();
        return Arrays.stream(allowedExtensions)
                .anyMatch(lowerFilename::endsWith);
    }

    private String getFileType(String filename) {
        if (filename == null) return "File";
        if (filename.endsWith(".pdf")) return "PDF";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "Image";
        if (filename.endsWith(".png")) return "Image";
        if (filename.endsWith(".doc") || filename.endsWith(".docx")) return "Document";
        return "File";
    }

    private boolean isLabResult(DossierMedical document) {
        if (document == null) return false;
        String title = document.getTitle() != null ? document.getTitle().toLowerCase() : "";
        String desc = document.getDescription() != null ? document.getDescription().toLowerCase() : "";
        return title.contains("lab") || title.contains("test") || desc.contains("lab") ||
                desc.contains("=== RÉSULTATS DE LABORATOIRE ===");
    }

    private String extractTestType(DossierMedical document) {
        if (document == null || document.getTitle() == null) return "Other Tests";
        String title = document.getTitle();
        if (title.contains("Blood")) return "Blood Test";
        if (title.contains("Urine")) return "Urine Test";
        if (title.contains("X-Ray")) return "X-Ray";
        return "Other Tests";
    }

    private int calculateAge(Date birthDate) {
        if (birthDate == null) return 0;
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthDate);
        Calendar today = Calendar.getInstance();

        int age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private List<String> extractAllergies(String medicalHistory) {
        if (medicalHistory == null) return new ArrayList<>();
        List<String> allergies = new ArrayList<>();
        if (medicalHistory.toLowerCase().contains("pollen")) allergies.add("Pollen");
        if (medicalHistory.toLowerCase().contains("penicillin")) allergies.add("Penicillin");
        if (medicalHistory.toLowerCase().contains("antibiotic")) allergies.add("Antibiotics");
        return allergies;
    }

    private List<String> extractChronicConditions(List<Consultation> consultations) {
        List<String> conditions = new ArrayList<>();
        for (Consultation consultation : consultations) {
            if (consultation.getNotes() != null) {
                String notes = consultation.getNotes().toLowerCase();
                if (notes.contains("hypertension")) conditions.add("Hypertension");
                if (notes.contains("diabetes")) conditions.add("Diabetes");
                if (notes.contains("asthma")) conditions.add("Asthma");
            }
        }
        return conditions;
    }

    private List<String> extractCurrentMedications(List<Ordonance> prescriptions) {
        List<String> medications = new ArrayList<>();
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);

        for (Ordonance prescription : prescriptions) {
            if (prescription.getDateCreation() != null) {
                LocalDate prescriptionDate = prescription.getDateCreation().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();

                if (prescriptionDate.isAfter(oneMonthAgo)) {
                    if (prescription.getMedicaments() != null) {
                        medications.addAll(prescription.getMedicaments());
                    }
                }
            }
        }
        return medications;
    }

    private Date getLastConsultationDate(List<Consultation> consultations) {
        if (consultations == null || consultations.isEmpty()) return null;

        return consultations.stream()
                .filter(c -> c.getDate() != null)
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .findFirst()
                .map(Consultation::getDate)
                .orElse(null);
    }

    private LocalDate getNextAppointmentDate(List<RendezVous> appointments) {
        if (appointments == null || appointments.isEmpty()) return null;

        return appointments.stream()
                .filter(a -> a.getDate() != null && a.getDate().isAfter(LocalDate.now()))
                .map(RendezVous::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    public String getCategoryClass(String category) {
        if (category == null) return "other";
        switch (category) {
            case "Medical Reports": return "reports";
            case "Scans/Imaging": return "scans";
            case "Prescriptions": return "prescriptions";
            case "Vaccinations": return "vaccinations";
            default: return "other";
        }
    }
}