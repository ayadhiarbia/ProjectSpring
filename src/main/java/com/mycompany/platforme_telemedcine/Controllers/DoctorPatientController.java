package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/medecin/patients")
public class DoctorPatientController {

    private final PatientService patientService;
    private final RendezVousService rendezVousService;
    private final ConsultationService consultationService;
    private final MedecinService medecinService;

    @Autowired
    public DoctorPatientController(
            PatientService patientService,
            RendezVousService rendezVousService,
            ConsultationService consultationService,
            MedecinService medecinService) {
        this.patientService = patientService;
        this.rendezVousService = rendezVousService;
        this.consultationService = consultationService;
        this.medecinService = medecinService;
    }

    // GET: Doctor's patients dashboard
    @GetMapping
    public String getDoctorPatients(HttpSession session, Model model) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        // Get all patients who have approved appointments with this doctor
        List<Patient> patients = getPatientsWithApprovedAppointments(doctor.getId());

        // For each patient, determine their last visit date and status
        List<Map<String, Object>> patientData = new ArrayList<>();
        for (Patient patient : patients) {
            Map<String, Object> patientInfo = new HashMap<>();
            patientInfo.put("patient", patient);
            patientInfo.put("lastVisitDate", getLastVisitDate(patient.getId(), doctor.getId()));
            patientInfo.put("isActive", isPatientActive(patient.getId(), doctor.getId()));
            patientData.add(patientInfo);
        }

        // Get statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPatients", patients.size());
        stats.put("activePatients", getActivePatientsCount(doctor.getId()));
        stats.put("todayAppointments", getTodayAppointmentsCount(doctor.getId()));
        stats.put("pendingConsultations", getPendingConsultationsCount(doctor.getId()));

        model.addAttribute("doctor", doctor);
        model.addAttribute("patientData", patientData);
        model.addAttribute("stats", stats);
        model.addAttribute("today", LocalDate.now());

        return "medecin/patients";
    }

    // GET: Patient details
    @GetMapping("/{id}")
    public String viewPatientDetails(@PathVariable Long id, HttpSession session, Model model) {
        Object user = session.getAttribute("user");

        if (!(user instanceof Medecin)) {
            return "redirect:/login?error=access-denied";
        }

        Medecin doctor = (Medecin) user;

        Patient patient = patientService.getPatientById(id);
        if (patient == null) {
            return "redirect:/medecin/patients?error=patientNotFound";
        }

        // Verify this patient has appointments with this doctor
        if (!hasAppointmentsWithDoctor(patient.getId(), doctor.getId())) {
            return "redirect:/medecin/patients?error=patientNotYours";
        }

        // Get patient's appointments with this doctor
        List<RendezVous> appointments = getPatientAppointmentsWithDoctor(patient.getId(), doctor.getId());

        // Get patient's consultations with this doctor
        List<Consultation> consultations = consultationService.getConsultationsByPatientAndMedecin(patient.getId(), doctor.getId());

        model.addAttribute("doctor", doctor);
        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointments);
        model.addAttribute("consultations", consultations);

        return "medecin/patient-details";
    }

    // Helper methods
    private List<Patient> getPatientsWithApprovedAppointments(Long doctorId) {
        // Get all appointments for this doctor
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        List<RendezVous> appointments = objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .filter(app -> app.getStatus() == StatusRendezVous.APPROVED ||
                        app.getStatus() == StatusRendezVous.COMPLETED ||
                        app.getStatus() == StatusRendezVous.IN_PROGRESS)
                .collect(Collectors.toList());

        // Extract unique patients
        Set<Patient> uniquePatients = new HashSet<>();
        for (RendezVous app : appointments) {
            if (app.getPatient() != null) {
                uniquePatients.add(app.getPatient());
            }
        }

        return new ArrayList<>(uniquePatients);
    }

    private Date getLastVisitDate(Long patientId, Long doctorId) {
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        Optional<RendezVous> lastAppointment = objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .filter(app -> app.getPatient() != null &&
                        app.getPatient().getId().equals(patientId) &&
                        app.getDate() != null &&
                        (app.getStatus() == StatusRendezVous.COMPLETED ||
                                app.getStatus() == StatusRendezVous.IN_PROGRESS))
                .max((a1, a2) -> a1.getDate().compareTo(a2.getDate()));

        if (lastAppointment.isPresent()) {
            // Convert LocalDate to Date
            LocalDate localDate = lastAppointment.get().getDate();
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }

        return null;
    }

    private boolean isPatientActive(Long patientId, Long doctorId) {
        // A patient is considered active if they have an appointment in the last 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        return objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .anyMatch(app -> app.getPatient() != null &&
                        app.getPatient().getId().equals(patientId) &&
                        app.getDate() != null &&
                        (app.getDate().isAfter(thirtyDaysAgo) || app.getDate().equals(thirtyDaysAgo)) &&
                        (app.getStatus() == StatusRendezVous.APPROVED ||
                                app.getStatus() == StatusRendezVous.COMPLETED ||
                                app.getStatus() == StatusRendezVous.IN_PROGRESS));
    }

    private int getActivePatientsCount(Long doctorId) {
        List<Patient> patients = getPatientsWithApprovedAppointments(doctorId);
        return (int) patients.stream()
                .filter(p -> isPatientActive(p.getId(), doctorId))
                .count();
    }

    private int getTodayAppointmentsCount(Long doctorId) {
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        return (int) objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .filter(app -> app.getDate() != null &&
                        app.getDate().equals(LocalDate.now()) &&
                        app.getStatus() == StatusRendezVous.APPROVED)
                .count();
    }

    private int getPendingConsultationsCount(Long doctorId) {
        List<Consultation> consultations = consultationService.getAllConsultations();
        return (int) consultations.stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getMedecin() != null &&
                        c.getRendezVous().getMedecin().getId().equals(doctorId) &&
                        Boolean.TRUE.equals(c.getIsActive()))
                .count();
    }

    private List<RendezVous> getPatientAppointmentsWithDoctor(Long patientId, Long doctorId) {
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        return objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .filter(app -> app.getPatient() != null &&
                        app.getPatient().getId().equals(patientId))
                .sorted((a1, a2) -> a2.getDate().compareTo(a1.getDate())) // Most recent first
                .collect(Collectors.toList());
    }

    private boolean hasAppointmentsWithDoctor(Long patientId, Long doctorId) {
        Collection<?> objects = rendezVousService.getRendezVousByMedecin(doctorId);
        return objects.stream()
                .filter(obj -> obj instanceof RendezVous)
                .map(obj -> (RendezVous) obj)
                .anyMatch(app -> app.getPatient() != null &&
                        app.getPatient().getId().equals(patientId) &&
                        (app.getStatus() == StatusRendezVous.APPROVED ||
                                app.getStatus() == StatusRendezVous.COMPLETED ||
                                app.getStatus() == StatusRendezVous.IN_PROGRESS));
    }
}