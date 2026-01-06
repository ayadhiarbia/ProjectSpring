package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.ConsultationRepository;
import com.mycompany.platforme_telemedcine.Repository.RendezVousRepository;
import com.mycompany.platforme_telemedcine.Services.ConsultationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConsultationServiceImp implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final RendezVousRepository rendezVousRepository;

    @Autowired
    public ConsultationServiceImp(ConsultationRepository consultationRepository,
                                  RendezVousRepository rendezVousRepository) {
        this.consultationRepository = consultationRepository;
        this.rendezVousRepository = rendezVousRepository;
    }

    @Override
    public Consultation createConsultation(Consultation consultation) {
        return consultationRepository.save(consultation);
    }

    @Override
    public Consultation getConsultationById(Long id) {
        return consultationRepository.findById(id).orElse(null);
    }

    @Override
    public Consultation updateConsultation(Consultation consultation) {
        return consultationRepository.save(consultation);
    }

    @Override
    public void deleteConsultation(Long id) {
        consultationRepository.deleteById(id);
    }

    @Override
    public List<Consultation> getAllConsultations() {
        return consultationRepository.findAll();
    }

    @Override
    public List<Consultation> getTodayConsultationsForMedecin(Long medecinId) {
        Date today = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date tomorrow = Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Consultation> allConsultations = consultationRepository.findAll();
        return allConsultations.stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getMedecin() != null &&
                        c.getRendezVous().getMedecin().getId().equals(medecinId) &&
                        c.getDate() != null &&
                        c.getDate().after(today) &&
                        c.getDate().before(tomorrow))
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getWaitingConsultationsForMedecin(Long medecinId) {
        List<Consultation> allConsultations = consultationRepository.findAll();
        return allConsultations.stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getMedecin() != null &&
                        c.getRendezVous().getMedecin().getId().equals(medecinId) &&
                        Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getActiveConsultationsForMedecin(Long medecinId) {
        List<Consultation> allConsultations = consultationRepository.findAll();
        return allConsultations.stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getMedecin() != null &&
                        c.getRendezVous().getMedecin().getId().equals(medecinId) &&
                        Boolean.TRUE.equals(c.getIsActive()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getConsultationsByPatientAndMedecin(Long patientId, Long medecinId) {
        return consultationRepository.findByRendezVousPatientIdAndRendezVousMedecinId(patientId, medecinId);
    }

    @Override
    public Consultation getConsultationByRendezVousId(Long rendezVousId) {
        List<Consultation> allConsultations = consultationRepository.findAll();
        return allConsultations.stream()
                .filter(c -> c.getRendezVous() != null &&
                        c.getRendezVous().getId().equals(rendezVousId))
                .findFirst()
                .orElse(null);
    }

    // ============ NEW METHODS FOR CONSULTATION WORKFLOW ============

    @Override
    @Transactional
    public Consultation createPatientConsultationRequest(Long patientId, Long doctorId,
                                                         ConsultationType type, String reason,
                                                         String symptoms, Date preferredDate) {
        // This creates a consultation WITHOUT linking to an existing appointment
        Consultation consultation = new Consultation();
        consultation.setConsultationType(type);
        consultation.setReason(reason);
        consultation.setSymptoms(symptoms);
        consultation.setCreatedBy("PATIENT");
        consultation.setStatus(ConsultationStatus.PENDING);
        consultation.setRequestedDate(new Date());
        consultation.setPreferredDateTime(preferredDate);

        return consultationRepository.save(consultation);
    }

    @Override
    @Transactional
    public Consultation createPatientConsultationRequest(Long patientId, Long doctorId,
                                                         ConsultationType type, String reason,
                                                         String symptoms, Date preferredDate,
                                                         Long rendezVousId) {
        try {
            System.out.println("=== Creating consultation with appointment ID: " + rendezVousId + " ===");

            // 1. Find the existing rendez-vous
            RendezVous existingRdv = rendezVousRepository.findById(rendezVousId)
                    .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + rendezVousId));

            System.out.println("Found appointment: " + existingRdv.getId() +
                    " for patient: " + existingRdv.getPatient().getId() +
                    " with doctor: " + existingRdv.getMedecin().getId());

            // 2. Validate the appointment
            if (existingRdv.getPatient() == null || !existingRdv.getPatient().getId().equals(patientId)) {
                throw new RuntimeException("Appointment does not belong to this patient. Expected: " +
                        patientId + ", Found: " + (existingRdv.getPatient() != null ? existingRdv.getPatient().getId() : "null"));
            }
            if (existingRdv.getMedecin() == null || !existingRdv.getMedecin().getId().equals(doctorId)) {
                throw new RuntimeException("Appointment is not with this doctor. Expected: " +
                        doctorId + ", Found: " + (existingRdv.getMedecin() != null ? existingRdv.getMedecin().getId() : "null"));
            }
            if (existingRdv.getConsultation() != null) {
                throw new RuntimeException("Appointment already has a consultation");
            }
            if (existingRdv.getStatus() != StatusRendezVous.APPROVED) {
                throw new RuntimeException("Appointment must be approved. Current status: " +
                        existingRdv.getStatus());
            }

            // 3. Create the consultation
            Consultation consultation = new Consultation();
            consultation.setConsultationType(type);
            consultation.setReason(reason);
            consultation.setSymptoms(symptoms);
            consultation.setCreatedBy("PATIENT");
            consultation.setStatus(ConsultationStatus.PENDING);
            consultation.setRequestedDate(new Date());
            consultation.setPreferredDateTime(preferredDate);

            // Set date from appointment (convert LocalDate to Date)
            if (existingRdv.getDate() != null) {
                consultation.setDate(java.sql.Date.valueOf(existingRdv.getDate()));
            }

            // 4. Save the consultation FIRST to get an ID
            Consultation savedConsultation = consultationRepository.save(consultation);
            System.out.println("Saved consultation with ID: " + savedConsultation.getId());

            // 5. Link the consultation to the existing rendez-vous
            existingRdv.setConsultation(savedConsultation);

            // 6. Save the updated rendez-vous
            rendezVousRepository.save(existingRdv);
            System.out.println("Updated appointment with consultation ID: " + savedConsultation.getId());

            return savedConsultation;

        } catch (Exception e) {
            System.err.println("Error in createPatientConsultationRequest: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create consultation request: " + e.getMessage(), e);
        }
    }

    @Override
    public Consultation approveConsultation(Long consultationId, Date scheduledDate) {
        Consultation consultation = getConsultationById(consultationId);
        if (consultation != null) {
            consultation.approve(); // Uses the approve() method from Consultation model
            consultation.setScheduledDate(scheduledDate);

            // Update rendezvous status
            if (consultation.getRendezVous() != null) {
                consultation.getRendezVous().setStatus(StatusRendezVous.APPROVED);
            }

            return consultationRepository.save(consultation);
        }
        return null;
    }

    @Override
    public Consultation rejectConsultation(Long consultationId, String rejectionReason) {
        Consultation consultation = getConsultationById(consultationId);
        if (consultation != null) {
            consultation.reject(rejectionReason); // Uses the reject() method from Consultation model
            return consultationRepository.save(consultation);
        }
        return null;
    }

    @Override
    public Consultation startConsultation(Long consultationId) {
        Consultation consultation = getConsultationById(consultationId);
        if (consultation != null && consultation.canStart()) {
            consultation.start(); // Uses the start() method from Consultation model
            consultation.setCallRoomId("room_" + consultationId + "_" + System.currentTimeMillis());
            return consultationRepository.save(consultation);
        }
        return null;
    }

    @Override
    public Consultation endConsultation(Long consultationId, String notes) {
        Consultation consultation = getConsultationById(consultationId);
        if (consultation != null && consultation.getStatus() == ConsultationStatus.IN_PROGRESS) {
            consultation.end(); // Uses the end() method from Consultation model
            if (notes != null && !notes.isEmpty()) {
                consultation.setNotes(notes);
            }
            return consultationRepository.save(consultation);
        }
        return null;
    }

    @Override
    public Consultation cancelConsultation(Long consultationId, String reason) {
        Consultation consultation = getConsultationById(consultationId);
        if (consultation != null && consultation.canCancel()) {
            consultation.cancel(reason); // Uses the cancel() method from Consultation model
            return consultationRepository.save(consultation);
        }
        return null;
    }

    @Override
    public Consultation rescheduleConsultation(Long consultationId, Date newDate) {
        Consultation consultation = getConsultationById(consultationId);
        if (consultation != null && consultation.canReschedule()) {
            consultation.reschedule(newDate); // Uses the reschedule() method from Consultation model
            return consultationRepository.save(consultation);
        }
        return null;
    }

    @Override
    public List<Consultation> getConsultationsByStatus(ConsultationStatus status) {
        return consultationRepository.findByStatus(status);
    }

    @Override
    public List<Consultation> getConsultationsByPatientIdAndStatus(Long patientId, ConsultationStatus status) {
        return consultationRepository.findByRendezVousPatientIdAndStatus(patientId, status);
    }

    @Override
    public List<Consultation> getConsultationsByDoctorIdAndStatus(Long doctorId, ConsultationStatus status) {
        return consultationRepository.findByRendezVousMedecinIdAndStatus(doctorId, status);
    }

    @Override
    public List<Consultation> getUpcomingConsultationsForPatient(Long patientId) {
        Date now = new Date();
        List<ConsultationStatus> upcomingStatuses = Arrays.asList(
                ConsultationStatus.APPROVED,
                ConsultationStatus.SCHEDULED
        );

        return consultationRepository.findByRendezVousPatientId(patientId).stream()
                .filter(c -> upcomingStatuses.contains(c.getStatus()) &&
                        c.getScheduledDate() != null &&
                        c.getScheduledDate().after(now))
                .sorted(Comparator.comparing(Consultation::getScheduledDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getUpcomingConsultationsForDoctor(Long doctorId) {
        Date now = new Date();
        List<ConsultationStatus> upcomingStatuses = Arrays.asList(
                ConsultationStatus.APPROVED,
                ConsultationStatus.SCHEDULED
        );

        return consultationRepository.findByRendezVousMedecinId(doctorId).stream()
                .filter(c -> upcomingStatuses.contains(c.getStatus()) &&
                        c.getScheduledDate() != null &&
                        c.getScheduledDate().after(now))
                .sorted(Comparator.comparing(Consultation::getScheduledDate))
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getPendingConsultationsForDoctor(Long doctorId) {
        return consultationRepository.findByRendezVousMedecinIdAndStatus(doctorId, ConsultationStatus.PENDING);
    }

    @Override
    public List<Consultation> getCompletedConsultationsForPatient(Long patientId) {
        return consultationRepository.findByRendezVousPatientIdAndStatus(patientId, ConsultationStatus.COMPLETED);
    }

    @Override
    public List<Consultation> getCompletedConsultationsForDoctor(Long doctorId) {
        return consultationRepository.findByRendezVousMedecinIdAndStatus(doctorId, ConsultationStatus.COMPLETED);
    }

    @Override
    public List<Consultation> getConsultationHistoryForPatient(Long patientId) {
        List<Consultation> consultations = consultationRepository.findByRendezVousPatientId(patientId);
        return consultations.stream()
                .sorted(Comparator.comparing(Consultation::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getConsultationHistoryForDoctor(Long doctorId) {
        List<Consultation> consultations = consultationRepository.findByRendezVousMedecinId(doctorId);
        return consultations.stream()
                .sorted(Comparator.comparing(Consultation::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public boolean canStartConsultation(Long consultationId) {
        Consultation consultation = getConsultationById(consultationId);
        return consultation != null && consultation.canStart();
    }

    @Override
    public boolean canCancelConsultation(Long consultationId) {
        Consultation consultation = getConsultationById(consultationId);
        return consultation != null && consultation.canCancel();
    }

    @Override
    public boolean canRescheduleConsultation(Long consultationId) {
        Consultation consultation = getConsultationById(consultationId);
        return consultation != null && consultation.canReschedule();
    }

    @Override
    public long countConsultationsByPatientAndStatus(Long patientId, ConsultationStatus status) {
        return consultationRepository.findByRendezVousPatientIdAndStatus(patientId, status).size();
    }

    @Override
    public long countConsultationsByDoctorAndStatus(Long doctorId, ConsultationStatus status) {
        return consultationRepository.findByRendezVousMedecinIdAndStatus(doctorId, status).size();
    }
}