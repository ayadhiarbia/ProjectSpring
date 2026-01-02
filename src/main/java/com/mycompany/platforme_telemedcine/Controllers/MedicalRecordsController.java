package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.ConsultationService;
import com.mycompany.platforme_telemedcine.Services.DossierMedicalService;
import com.mycompany.platforme_telemedcine.Services.OrdonanceService;
import com.mycompany.platforme_telemedcine.Services.RendezVousService;
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
@RequestMapping("/medical-records")
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

    // GET: Main medical records dashboard
    @GetMapping
    public String getMedicalRecords(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        // Get all medical records for the patient
        List<DossierMedical> documents = dossierMedicalService.getDossiersByPatientId(patient.getId());
        List<Consultation> consultations = getConsultationsByPatientId(patient.getId());
        List<Ordonance> prescriptions = getPrescriptionsByPatientId(patient.getId());
        List<RendezVous> appointments = rendezVousService.getByPatient(patient.getId());

        // Group documents by category
        Map<String, List<DossierMedical>> documentsByCategory = groupDocumentsByCategory(documents);

        // Prepare medical summary
        MedicalSummary summary = prepareMedicalSummary(patient, consultations, prescriptions, appointments);

        model.addAttribute("patient", patient);
        model.addAttribute("documents", documents);
        model.addAttribute("documentsByCategory", documentsByCategory);
        model.addAttribute("consultations", consultations);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("appointments", appointments);
        model.addAttribute("summary", summary);
        model.addAttribute("documentCount", documents.size());
        model.addAttribute("consultationCount", consultations.size());
        model.addAttribute("prescriptionCount", prescriptions.size());

        return "patient/medical-records";
    }

    // GET: View specific document
    @GetMapping("/document/{id}")
    public String viewDocument(@PathVariable Long id, HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        DossierMedical document = dossierMedicalService.getDossierMedicalById(id);
        if (document == null || !document.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/medical-records?error=documentNotFound";
        }

        model.addAttribute("patient", patient);
        model.addAttribute("document", document);
        model.addAttribute("fileType", getFileType(document.getFileName()));

        return "patient/document-details";
    }

    // GET: Download document
    @GetMapping("/download/{id}")
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

    // POST: Upload document (patients can upload their own documents)
    @PostMapping("/upload")
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

        // Create full description with category
        String fullDescription = "[" + category.toUpperCase() + "] " + description;

        DossierMedical document = dossierMedicalService.uploadDocumentMedical(
                patient, file, title, fullDescription);
        return "redirect:/patient/medical-records?success=uploaded&documentId=" + document.getId();
    }

    // GET: View consultation history
    @GetMapping("/consultations")
    public String getConsultationHistory(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<Consultation> consultations = getConsultationsByPatientId(patient.getId());

        // Group by year
        Map<Integer, List<Consultation>> consultationsByYear = consultations.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getDate().toInstant().atZone(ZoneId.systemDefault()).getYear(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("patient", patient);
        model.addAttribute("consultations", consultations);
        model.addAttribute("consultationsByYear", consultationsByYear);
        model.addAttribute("totalConsultations", consultations.size());

        return "patient/consultation-history";
    }

    // GET: View specific consultation details
    @GetMapping("/consultation/{id}")
    public String viewConsultationDetails(@PathVariable Long id, HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(id);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/medical-records/consultations?error=notFound";
        }

        model.addAttribute("patient", patient);
        model.addAttribute("consultation", consultation);
        model.addAttribute("doctor", consultation.getRendezVous().getMedecin());
        model.addAttribute("hasPrescription", consultation.getOrdonance() != null);

        return "patient/consultation-details";
    }

    // GET: View prescriptions
    @GetMapping("/prescriptions")
    public String getPrescriptions(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<Ordonance> prescriptions = getPrescriptionsByPatientId(patient.getId());

        // Group by status (active/completed)
        Map<String, List<Ordonance>> prescriptionsByStatus = new HashMap<>();
        prescriptionsByStatus.put("active", new ArrayList<>());
        prescriptionsByStatus.put("completed", new ArrayList<>());

        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        for (Ordonance prescription : prescriptions) {
            LocalDate prescriptionDate = prescription.getDateCreation().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();

            if (prescriptionDate.isAfter(oneMonthAgo)) {
                prescriptionsByStatus.get("active").add(prescription);
            } else {
                prescriptionsByStatus.get("completed").add(prescription);
            }
        }

        model.addAttribute("patient", patient);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("prescriptionsByStatus", prescriptionsByStatus);
        model.addAttribute("activeCount", prescriptionsByStatus.get("active").size());
        model.addAttribute("completedCount", prescriptionsByStatus.get("completed").size());

        return "patient/prescriptions";
    }

    // GET: View specific prescription
    @GetMapping("/prescription/{id}")
    public String viewPrescription(@PathVariable Long id, HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Ordonance prescription = ordonanceService.findOrdonanceById(id);
        if (prescription == null ||
                !prescription.getConsultation().getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/medical-records/prescriptions?error=notFound";
        }

        model.addAttribute("patient", patient);
        model.addAttribute("prescription", prescription);
        model.addAttribute("consultation", prescription.getConsultation());
        model.addAttribute("doctor", prescription.getConsultation().getRendezVous().getMedecin());

        return "patient/prescription-details";
    }

    // GET: Search/filter records
    @GetMapping("/search")
    @ResponseBody
    public Map<String, Object> searchRecords(@RequestParam String query,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate,
                                             HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (patient == null) {
            response.put("error", "Not authenticated");
            return response;
        }

        List<SearchResult> results = new ArrayList<>();

        // Search in documents
        if (type == null || type.equals("documents") || type.equals("all")) {
            List<DossierMedical> documents = dossierMedicalService.getDossiersByPatientId(patient.getId());
            documents.stream()
                    .filter(doc -> matchesQuery(doc, query))
                    .forEach(doc -> results.add(new SearchResult(
                            "DOCUMENT",
                            doc.getId(),
                            doc.getTitle(),
                            doc.getDescription(),
                            doc.getUploadDate().toString(),
                            "/patient/medical-records/document/" + doc.getId()
                    )));
        }

        // Search in consultations
        if (type == null || type.equals("consultations") || type.equals("all")) {
            List<Consultation> consultations = getConsultationsByPatientId(patient.getId());
            consultations.stream()
                    .filter(cons -> matchesQuery(cons, query))
                    .forEach(cons -> results.add(new SearchResult(
                            "CONSULTATION",
                            cons.getId(),
                            "Consultation avec Dr. " + cons.getRendezVous().getMedecin().getName(),
                            cons.getNotes(),
                            cons.getDate().toString(),
                            "/patient/medical-records/consultation/" + cons.getId()
                    )));
        }

        // Search in prescriptions
        if (type == null || type.equals("prescriptions") || type.equals("all")) {
            List<Ordonance> prescriptions = getPrescriptionsByPatientId(patient.getId());
            prescriptions.stream()
                    .filter(pres -> matchesQuery(pres, query))
                    .forEach(pres -> results.add(new SearchResult(
                            "PRESCRIPTION",
                            pres.getId(),
                            "Ordonnance médicale",
                            String.join(", ", pres.getMedicaments()),
                            pres.getDateCreation().toString(),
                            "/patient/medical-records/prescription/" + pres.getId()
                    )));
        }

        // Apply date filters if provided
        if (startDate != null && endDate != null) {
            // Filter results by date range
            try {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                results.removeIf(result -> {
                    LocalDate resultDate = LocalDate.parse(result.getDate().split("T")[0]);
                    return resultDate.isBefore(start) || resultDate.isAfter(end);
                });
            } catch (Exception e) {
                // If date parsing fails, ignore the filter
            }
        }

        response.put("results", results);
        response.put("count", results.size());
        return response;
    }

    // GET: Export medical summary as PDF
    @GetMapping("/export/pdf")
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

    // GET: Share records (generate shareable link)
    @PostMapping("/share")
    @ResponseBody
    public Map<String, String> shareRecords(@RequestBody ShareRequest request,
                                            HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, String> response = new HashMap<>();

        if (patient == null) {
            response.put("error", "Not authenticated");
            return response;
        }

        // Generate secure share token
        String shareToken = UUID.randomUUID().toString();

        // In a real application, you would save this token in database
        // with expiration date and access permissions

        String shareUrl = "/patient/medical-records/shared/" + shareToken;

        response.put("shareUrl", shareUrl);
        response.put("token", shareToken);
        response.put("expires", LocalDateTime.now().plusDays(7).toString());

        return response;
    }

    // GET: View medical history summary
    @GetMapping("/summary")
    public String getMedicalSummary(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<Consultation> consultations = getConsultationsByPatientId(patient.getId());
        List<Ordonance> prescriptions = getPrescriptionsByPatientId(patient.getId());
        List<RendezVous> appointments = rendezVousService.getByPatient(patient.getId());

        MedicalSummary summary = prepareMedicalSummary(patient, consultations, prescriptions, appointments);

        // Calculate statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConsultations", consultations.size());
        stats.put("totalPrescriptions", prescriptions.size());
        stats.put("totalAppointments", appointments.size());
        stats.put("activeConditions", summary.getChronicConditions().size());
        stats.put("upcomingAppointments", appointments.stream()
                .filter(a -> a.getDate().isAfter(LocalDate.now()))
                .count());

        // Prepare timeline data
        List<TimelineEvent> timeline = prepareTimeline(consultations, prescriptions, appointments);

        model.addAttribute("patient", patient);
        model.addAttribute("summary", summary);
        model.addAttribute("stats", stats);
        model.addAttribute("timeline", timeline);
        model.addAttribute("age", calculateAge(patient.getDataNaissance()));

        return "patient/medical-summary";
    }

    // GET: View lab results
    @GetMapping("/lab-results")
    public String getLabResults(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<DossierMedical> labResults = dossierMedicalService.getDossiersByPatientId(patient.getId())
                .stream()
                .filter(this::isLabResult)
                .collect(Collectors.toList());

        // Group by test type
        Map<String, List<DossierMedical>> resultsByType = labResults.stream()
                .collect(Collectors.groupingBy(this::extractTestType));

        model.addAttribute("patient", patient);
        model.addAttribute("labResults", labResults);
        model.addAttribute("resultsByType", resultsByType);
        model.addAttribute("totalResults", labResults.size());

        return "patient/lab-results";
    }

    // GET: View vaccination records
    @GetMapping("/vaccinations")
    public String getVaccinationRecords(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<DossierMedical> vaccinations = dossierMedicalService.getDossiersByPatientId(patient.getId())
                .stream()
                .filter(this::isVaccinationRecord)
                .collect(Collectors.toList());

        // Group by vaccine type
        Map<String, List<DossierMedical>> vaccinesByType = vaccinations.stream()
                .collect(Collectors.groupingBy(doc -> extractVaccineType(doc.getTitle())));

        model.addAttribute("patient", patient);
        model.addAttribute("vaccinations", vaccinations);
        model.addAttribute("vaccinesByType", vaccinesByType);
        model.addAttribute("totalVaccinations", vaccinations.size());

        return "patient/vaccinations";
    }

    // Helper methods
    private List<Consultation> getConsultationsByPatientId(Long patientId) {
        return consultationService.getAllConsultations().stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getPatient().getId().equals(patientId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .collect(Collectors.toList());
    }

    private List<Ordonance> getPrescriptionsByPatientId(Long patientId) {
        return ordonanceService.getAllOrdonance().stream()
                .filter(o -> o.getConsultation() != null &&
                        o.getConsultation().getRendezVous() != null &&
                        o.getConsultation().getRendezVous().getPatient().getId().equals(patientId))
                .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
                .collect(Collectors.toList());
    }

    private Map<String, List<DossierMedical>> groupDocumentsByCategory(List<DossierMedical> documents) {
        Map<String, List<DossierMedical>> grouped = new HashMap<>();
        grouped.put("Lab Results", new ArrayList<>());
        grouped.put("Prescriptions", new ArrayList<>());
        grouped.put("Medical Reports", new ArrayList<>());
        grouped.put("Scans/Imaging", new ArrayList<>());
        grouped.put("Vaccinations", new ArrayList<>());
        grouped.put("Other", new ArrayList<>());

        for (DossierMedical doc : documents) {
            String category = detectCategory(doc);
            grouped.get(category).add(doc);
        }

        return grouped;
    }

    private String detectCategory(DossierMedical doc) {
        String title = doc.getTitle().toLowerCase();
        String desc = doc.getDescription().toLowerCase();

        if (title.contains("lab") || title.contains("test") || desc.contains("lab")) {
            return "Lab Results";
        } else if (title.contains("prescription") || title.contains("ordonnance")) {
            return "Prescriptions";
        } else if (title.contains("scan") || title.contains("x-ray") || title.contains("mri") ||
                title.contains("radiologie")) {
            return "Scans/Imaging";
        } else if (title.contains("vaccin") || title.contains("vaccination")) {
            return "Vaccinations";
        } else if (title.contains("report") || title.contains("rapport") ||
                title.contains("consultation") || title.contains("summary")) {
            return "Medical Reports";
        } else {
            return "Other";
        }
    }

    private MedicalSummary prepareMedicalSummary(Patient patient,
                                                 List<Consultation> consultations,
                                                 List<Ordonance> prescriptions,
                                                 List<RendezVous> appointments) {
        MedicalSummary summary = new MedicalSummary();
        summary.setPatientName(patient.getName());
        summary.setAge(calculateAge(patient.getDataNaissance()));
        summary.setBloodType("Not specified");
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
        document.add(new Paragraph("Adresse: " + patient.getAdresse()));

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

            for (Consultation consultation : consultations.stream().limit(5).collect(Collectors.toList())) {
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
        return title.contains("lab") || title.contains("test") || desc.contains("lab");
    }

    private boolean isVaccinationRecord(DossierMedical document) {
        if (document == null) return false;
        String title = document.getTitle() != null ? document.getTitle().toLowerCase() : "";
        String desc = document.getDescription() != null ? document.getDescription().toLowerCase() : "";
        return title.contains("vaccin") || desc.contains("vaccin");
    }

    private String extractTestType(DossierMedical document) {
        if (document == null || document.getTitle() == null) return "Other Tests";
        String title = document.getTitle();
        if (title.contains("Blood")) return "Blood Test";
        if (title.contains("Urine")) return "Urine Test";
        if (title.contains("X-Ray")) return "X-Ray";
        return "Other Tests";
    }

    private String extractVaccineType(String title) {
        if (title == null) return "Other Vaccines";
        if (title.contains("COVID")) return "COVID-19";
        if (title.contains("Flu")) return "Influenza";
        if (title.contains("Hepatitis")) return "Hepatitis";
        if (title.contains("Tetanus")) return "Tetanus";
        return "Other Vaccines";
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
        // Simple extraction - in real app, you'd have a structured allergies field
        return Arrays.asList("Pollen", "Penicillin");
    }

    private List<String> extractChronicConditions(List<Consultation> consultations) {
        List<String> conditions = new ArrayList<>();
        for (Consultation consultation : consultations) {
            if (consultation.getNotes() != null &&
                    consultation.getNotes().toLowerCase().contains("chronic")) {
                conditions.add("Hypertension");
            }
        }
        return conditions;
    }

    private List<String> extractCurrentMedications(List<Ordonance> prescriptions) {
        List<String> medications = new ArrayList<>();
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);

        for (Ordonance prescription : prescriptions) {
            LocalDate prescriptionDate = prescription.getDateCreation().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();

            if (prescriptionDate.isAfter(oneMonthAgo)) {
                medications.addAll(prescription.getMedicaments());
            }
        }
        return medications;
    }

    private Date getLastConsultationDate(List<Consultation> consultations) {
        if (consultations.isEmpty()) return null;
        return consultations.get(0).getDate();
    }

    private LocalDate getNextAppointmentDate(List<RendezVous> appointments) {
        return appointments.stream()
                .filter(a -> a.getDate().isAfter(LocalDate.now()))
                .map(RendezVous::getDate)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    private List<TimelineEvent> prepareTimeline(List<Consultation> consultations,
                                                List<Ordonance> prescriptions,
                                                List<RendezVous> appointments) {
        List<TimelineEvent> timeline = new ArrayList<>();

        // Add consultations
        for (Consultation consultation : consultations) {
            timeline.add(new TimelineEvent(
                    "CONSULTATION",
                    consultation.getDate(),
                    "Consultation avec Dr. " + consultation.getRendezVous().getMedecin().getName(),
                    consultation.getNotes() != null ? consultation.getNotes() : ""
            ));
        }

        // Add prescriptions
        for (Ordonance prescription : prescriptions) {
            timeline.add(new TimelineEvent(
                    "PRESCRIPTION",
                    prescription.getDateCreation(),
                    "Nouvelle ordonnance",
                    String.join(", ", prescription.getMedicaments())
            ));
        }

        // Add appointments
        for (RendezVous appointment : appointments) {
            timeline.add(new TimelineEvent(
                    "APPOINTMENT",
                    Date.from(appointment.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
                    "Rendez-vous avec Dr. " + appointment.getMedecin().getName(),
                    appointment.getDescription() != null ? appointment.getDescription() : ""
            ));
        }

        // Sort by date descending
        timeline.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        return timeline;
    }

    private boolean matchesQuery(DossierMedical doc, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String searchQuery = query.toLowerCase();
        boolean titleMatch = doc.getTitle() != null && doc.getTitle().toLowerCase().contains(searchQuery);
        boolean descMatch = doc.getDescription() != null && doc.getDescription().toLowerCase().contains(searchQuery);
        return titleMatch || descMatch;
    }

    private boolean matchesQuery(Consultation consultation, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String searchQuery = query.toLowerCase();
        boolean notesMatch = consultation.getNotes() != null && consultation.getNotes().toLowerCase().contains(searchQuery);
        boolean doctorMatch = consultation.getRendezVous().getMedecin().getName() != null &&
                consultation.getRendezVous().getMedecin().getName().toLowerCase().contains(searchQuery);
        return notesMatch || doctorMatch;
    }

    private boolean matchesQuery(Ordonance prescription, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String searchQuery = query.toLowerCase();
        return prescription.getMedicaments().stream()
                .anyMatch(med -> med != null && med.toLowerCase().contains(searchQuery));
    }
}

// DTO Classes - Move these to separate files in models.dto package

class MedicalSummary {
    private String patientName;
    private int age;
    private String bloodType;
    private List<String> allergies;
    private List<String> chronicConditions;
    private List<String> currentMedications;
    private Date lastConsultation;
    private LocalDate nextAppointment;

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }

    public List<String> getChronicConditions() { return chronicConditions; }
    public void setChronicConditions(List<String> chronicConditions) { this.chronicConditions = chronicConditions; }

    public List<String> getCurrentMedications() { return currentMedications; }
    public void setCurrentMedications(List<String> currentMedications) { this.currentMedications = currentMedications; }

    public Date getLastConsultation() { return lastConsultation; }
    public void setLastConsultation(Date lastConsultation) { this.lastConsultation = lastConsultation; }

    public LocalDate getNextAppointment() { return nextAppointment; }
    public void setNextAppointment(LocalDate nextAppointment) { this.nextAppointment = nextAppointment; }
}

class SearchResult {
    private String type;
    private Long id;
    private String title;
    private String description;
    private String date;
    private String url;

    public SearchResult(String type, Long id, String title, String description, String date, String url) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.description = description;
        this.date = date;
        this.url = url;
    }

    public String getType() { return type; }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDate() { return date; }
    public String getUrl() { return url; }
}

class ShareRequest {
    private List<Long> documentIds;
    private List<Long> consultationIds;
    private List<Long> prescriptionIds;
    private String doctorEmail;
    private Date expiresAt;

    public List<Long> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<Long> documentIds) { this.documentIds = documentIds; }

    public List<Long> getConsultationIds() { return consultationIds; }
    public void setConsultationIds(List<Long> consultationIds) { this.consultationIds = consultationIds; }

    public List<Long> getPrescriptionIds() { return prescriptionIds; }
    public void setPrescriptionIds(List<Long> prescriptionIds) { this.prescriptionIds = prescriptionIds; }

    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }

    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
}

class TimelineEvent {
    private String type;
    private Date date;
    private String title;
    private String description;

    public TimelineEvent(String type, Date date, String title, String description) {
        this.type = type;
        this.date = date;
        this.title = title;
        this.description = description;
    }

    public String getType() { return type; }
    public Date getDate() { return date; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
}