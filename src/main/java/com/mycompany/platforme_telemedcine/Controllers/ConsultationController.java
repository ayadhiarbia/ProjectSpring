package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Services.*;
import com.mycompany.platforme_telemedcine.dto.*;
import com.mycompany.platforme_telemedcine.Controllers.helpers.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.sql.Date;


@Controller
@RequestMapping("/patient/consultation")
public class ConsultationController {

    private final ConsultationService consultationService;
    private final RendezVousService rendezVousService;
    private final OrdonanceService ordonanceService;
    private final DossierMedicalService dossierMedicalService;
    private final NotificationService notificationService;
    private final PatientService patientService;
    private final MedecinService medecinService;
    private final SimpMessagingTemplate messagingTemplate;

    // Track active consultations and waiting rooms
    private final Map<String, ConsultationSession> activeSessions = new HashMap<>();
    private final Map<String, WaitingRoom> waitingRooms = new HashMap<>();

    @Autowired
    public ConsultationController(ConsultationService consultationService,
                                         RendezVousService rendezVousService,
                                         OrdonanceService ordonanceService,
                                         DossierMedicalService dossierMedicalService,
                                         NotificationService notificationService,
                                         PatientService patientService,
                                         MedecinService medecinService,
                                         SimpMessagingTemplate messagingTemplate) {
        this.consultationService = consultationService;
        this.rendezVousService = rendezVousService;
        this.ordonanceService = ordonanceService;
        this.dossierMedicalService = dossierMedicalService;
        this.notificationService = notificationService;
        this.patientService = patientService;
        this.medecinService = medecinService;
        this.messagingTemplate = messagingTemplate;
    }

    // ============ CONSULTATION REQUEST WORKFLOW ============

    // GET: Request new consultation form
   // @GetMapping("/request")
    //public String requestConsultationForm(@RequestParam(required = false) Long doctorId,
                                    //      HttpSession session, Model model) {
        //Patient patient = (Patient) session.getAttribute("user");
     //   if (patient == null) {
           // return "redirect:/login";
       // }

      //  List<Medecin> doctors = medecinService.getAllMedecins();

      //  model.addAttribute("patient", patient);
        //model.addAttribute("doctors", doctors);
        //model.addAttribute("consultationTypes", ConsultationType.values());

       // if (doctorId != null) {
         //   Medecin selectedDoctor = medecinService.getMedecinById(doctorId);
           // model.addAttribute("selectedDoctor", selectedDoctor);
       // }

        //return "patient/request-consultation";
  //  }

    // POST: Submit consultation request
   // @PostMapping("/request")
   // public String submitConsultationRequest(
          //  @RequestParam Long doctorId,
           // @RequestParam ConsultationType consultationType,
          //  @RequestParam String reason,
          //  @RequestParam(required = false) String symptoms,
           // @DateTimeFormat(pattern = "yyyy-MM-dd")
            //@RequestParam(required = false) LocalDate preferredDate,
            //HttpSession session) {

       // Object user = session.getAttribute("user");
      //  if (!(user instanceof Patient patient)) {
         //   return "redirect:/login";
//        }

        //Date sqlDate = preferredDate != null ? Date.valueOf(preferredDate) : null;

      //  Consultation consultation = consultationService.createPatientConsultationRequest(
             //   patient.getId(),
             //   doctorId,
             //   consultationType,
              //  reason,
              //  symptoms != null ? symptoms : "",
              //  sqlDate
       // );

       // if (consultation != null) {

         //   if (notificationService != null) {
            //    notificationService.createNotification(
                     //   doctorId,
                      //  "Nouvelle demande de consultation",
                      //  patient.getName() + " a demandé une consultation " + consultationType,
                     //   "/doctor/consultation/pending"
               // );
           // }

           // return "redirect:/patient/consultation?success=requestSubmitted";
      //  }

      //  return "redirect:/patient/consultation/request?error=requestFailed";
   // }


    // GET: View all consultations for patient
    @GetMapping
    public String consultationDashboard(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        // Get consultations for this patient
        List<Consultation> consultations = consultationService.getConsultationsByPatientAndMedecin(
                patient.getId(), null); // Get all consultations for patient

        // Group by status
        List<Consultation> pendingConsultations = new ArrayList<>();
        List<Consultation> upcomingConsultations = new ArrayList<>();
        List<Consultation> completedConsultations = new ArrayList<>();
        List<Consultation> cancelledConsultations = new ArrayList<>();

        for (Consultation c : consultations) {
            if (c.getStatus() == ConsultationStatus.PENDING) {
                pendingConsultations.add(c);
            } else if (c.getStatus() == ConsultationStatus.SCHEDULED ||
                    c.getStatus() == ConsultationStatus.APPROVED) {
                upcomingConsultations.add(c);
            } else if (c.getStatus() == ConsultationStatus.COMPLETED) {
                completedConsultations.add(c);
            } else if (c.getStatus() == ConsultationStatus.CANCELLED ||
                    c.getStatus() == ConsultationStatus.REJECTED) {
                cancelledConsultations.add(c);
            }
        }

        model.addAttribute("patient", patient);
        model.addAttribute("pendingConsultations", pendingConsultations);
        model.addAttribute("upcomingConsultations", upcomingConsultations);
        model.addAttribute("completedConsultations", completedConsultations);
        model.addAttribute("cancelledConsultations", cancelledConsultations);

        return "patient/consultation-dashboard";
    }

    // GET: View consultation details
    @GetMapping("/{consultationId}")
    public String viewConsultation(@PathVariable Long consultationId,
                                   HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/consultation?error=accessDenied";
        }

        boolean canCancel = consultationService.canCancelConsultation(consultationId);
        boolean canReschedule = consultationService.canRescheduleConsultation(consultationId);
        boolean canStart = consultationService.canStartConsultation(consultationId);

        model.addAttribute("patient", patient);
        model.addAttribute("consultation", consultation);
        model.addAttribute("doctor", consultation.getRendezVous().getMedecin());
        model.addAttribute("canCancel", canCancel);
        model.addAttribute("canReschedule", canReschedule);
        model.addAttribute("canStart", canStart);

        return "patient/consultation-details";
    }

    // POST: Cancel consultation
    @PostMapping("/{consultationId}/cancel")
    public String cancelConsultation(@PathVariable Long consultationId,
                                     @RequestParam String reason,
                                     HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/consultation?error=accessDenied";
        }

        if (!consultationService.canCancelConsultation(consultationId)) {
            return "redirect:/patient/consultation/" + consultationId + "?error=cannotCancel";
        }

        Consultation cancelled = consultationService.cancelConsultation(consultationId, reason);
        if (cancelled != null) {
            // Notify doctor
            if (notificationService != null) {
                notificationService.createNotification(
                        consultation.getRendezVous().getMedecin().getId(),
                        "Consultation annulée",
                        patient.getName() + " a annulé la consultation",
                        "/doctor/consultation"
                );
            }
            return "redirect:/patient/consultation?success=cancelled";
        }

        return "redirect:/patient/consultation/" + consultationId + "?error=cancelFailed";
    }

    // GET: Reschedule consultation form
    @GetMapping("/{consultationId}/reschedule")
    public String rescheduleForm(@PathVariable Long consultationId,
                                 HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/consultation?error=accessDenied";
        }

        if (!consultationService.canRescheduleConsultation(consultationId)) {
            return "redirect:/patient/consultation/" + consultationId + "?error=cannotReschedule";
        }

        model.addAttribute("patient", patient);
        model.addAttribute("consultation", consultation);
        model.addAttribute("doctor", consultation.getRendezVous().getMedecin());

        return "patient/reschedule-consultation";
    }

    // POST: Reschedule consultation
    @PostMapping("/{consultationId}/reschedule")
    public String rescheduleConsultation(
            @PathVariable Long consultationId,
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            @RequestParam LocalDate newDate,
            @RequestParam String reason,
            HttpSession session) {

        Object user = session.getAttribute("user");
        if (!(user instanceof Patient patient)) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);

        if (consultation == null ||
                consultation.getRendezVous() == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/consultation?error=accessDenied";
        }

        if (!consultationService.canRescheduleConsultation(consultationId)) {
            return "redirect:/patient/consultation/" + consultationId + "?error=cannotReschedule";
        }

        Date sqlDate = Date.valueOf(newDate);
        Consultation rescheduled = consultationService.rescheduleConsultation(consultationId, sqlDate);

        if (rescheduled != null) {

            String notes = rescheduled.getNotes() != null ? rescheduled.getNotes() : "";
            notes += "\n\nRescheduling reason: " + reason;
            rescheduled.setNotes(notes);
            consultationService.updateConsultation(rescheduled);

            if (notificationService != null) {
                notificationService.createNotification(
                        consultation.getRendezVous().getMedecin().getId(),
                        "Consultation reprogrammée",
                        patient.getName() + " a demandé à reprogrammer la consultation",
                        "/doctor/consultation/pending"
                );
            }

            return "redirect:/patient/consultation?success=rescheduled";
        }

        return "redirect:/patient/consultation/" + consultationId + "/reschedule?error=rescheduleFailed";
    }

    // ============ CONSULTATION ROOM FUNCTIONALITY ============

    // GET: Join consultation room
    @GetMapping("/room/{consultationId}")
    public String consultationRoom(@PathVariable Long consultationId,
                                   HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/consultation?error=invalidConsultation";
        }

        // Check if consultation can be started
        if (!consultationService.canStartConsultation(consultationId)) {
            return "redirect:/patient/consultation/" + consultationId + "?error=cannotStart";
        }

        Medecin doctor = consultation.getRendezVous().getMedecin();

        // Check if consultation is active
        boolean isActive = consultation.getIsActive() != null && consultation.getIsActive();
        boolean isInProgress = consultation.getStatus() == ConsultationStatus.IN_PROGRESS;

        // Get or create session
        String roomId = consultation.getCallRoomId();
        if (roomId == null) {
            roomId = "consultation_" + consultationId + "_" + System.currentTimeMillis();
            consultation.setCallRoomId(roomId);
            consultationService.updateConsultation(consultation);
        }

        // Create waiting room if not exists
        if (!waitingRooms.containsKey(roomId)) {
            waitingRooms.put(roomId, new WaitingRoom(roomId, consultationId));
        }

        // Add patient to waiting room if not already active
        if (!isActive && !isInProgress) {
            waitingRooms.get(roomId).addParticipant(patient.getId(), patient.getName(), "PATIENT");
        }

        model.addAttribute("consultation", consultation);
        model.addAttribute("patient", patient);
        model.addAttribute("doctor", doctor);
        model.addAttribute("roomId", roomId);
        model.addAttribute("isActive", isActive);
        model.addAttribute("isInProgress", isInProgress);
        model.addAttribute("consultationType", consultation.getConsultationType());
        model.addAttribute("isInWaitingRoom", !isActive && !isInProgress);

        // Add WebRTC configuration
        model.addAttribute("stunServers", getStunServers());
        model.addAttribute("turnServers", getTurnServers());

        return "patient/consultation-room";
    }

    // POST: Join consultation (move from waiting room to active session)
    @PostMapping("/join/{consultationId}")
    @ResponseBody
    public Map<String, Object> joinConsultation(@PathVariable Long consultationId,
                                                HttpSession session) {
        Patient patient = (Patient) session.getAttribute("user");
        Map<String, Object> response = new HashMap<>();

        if (patient == null) {
            response.put("success", false);
            response.put("error", "Not authenticated");
            return response;
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            response.put("success", false);
            response.put("error", "Invalid consultation");
            return response;
        }

        String roomId = consultation.getCallRoomId();

        // Remove from waiting room
        if (waitingRooms.containsKey(roomId)) {
            waitingRooms.get(roomId).removeParticipant(patient.getId());

            // Notify doctor that patient has joined waiting room
            TimelineEventDTO event = new TimelineEventDTO();
            event.setEventType("PATIENT_WAITING");
            event.setConsultationId(consultationId);
            event.setUserId(patient.getId());
            event.setDescription("Patient en salle d'attente");

            messagingTemplate.convertAndSend(
                    "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/consultation",
                    event
            );
        }

        response.put("success", true);
        response.put("roomId", roomId);
        response.put("consultationType", consultation.getConsultationType());
        return response;
    }

    // ============ CONSULTATION HISTORY & SUMMARY ============

    // GET: Consultation history
    @GetMapping("/history")
    public String consultationHistory(HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        List<Consultation> consultations = consultationService.getConsultationHistoryForPatient(patient.getId());

        model.addAttribute("patient", patient);
        model.addAttribute("consultations", consultations);

        return "patient/consultation-history";
    }

    // GET: Consultation summary
    @GetMapping("/summary/{consultationId}")
    public String consultationSummary(@PathVariable Long consultationId,
                                      HttpSession session, Model model) {
        Patient patient = (Patient) session.getAttribute("user");
        if (patient == null) {
            return "redirect:/login";
        }

        Consultation consultation = consultationService.getConsultationById(consultationId);
        if (consultation == null ||
                !consultation.getRendezVous().getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/consultation?error=invalidConsultation";
        }

        model.addAttribute("patient", patient);
        model.addAttribute("consultation", consultation);
        model.addAttribute("doctor", consultation.getRendezVous().getMedecin());

        if (consultation.getOrdonance() != null) {
            model.addAttribute("prescription", consultation.getOrdonance());
        }

        return "patient/consultation-summary";
    }

    // ============ HELPER METHODS ============

    private List<String> getStunServers() {
        return Arrays.asList(
                "stun:stun.l.google.com:19302",
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302",
                "stun:stun3.l.google.com:19302",
                "stun:stun4.l.google.com:19302"
        );
    }

    private List<TurnServer> getTurnServers() {
        // In production, use real TURN servers with credentials
        return Arrays.asList(
                new TurnServer("turn:turn.example.com", "username", "password")
        );
    }

    // ============ WEBSOCKET HANDLERS (Keep existing ones) ============

    @MessageMapping("/consultation.start")
    public void startConsultation(@Payload ConsultationStartRequestDTO request) {
        Consultation consultation = consultationService.getConsultationById(request.getConsultationId());
        if (consultation != null) {
            // Start consultation using service method
            consultation = consultationService.startConsultation(consultation.getId());

            if (consultation != null) {
                String roomId = consultation.getCallRoomId();

                // Create active session
                ConsultationSession session = new ConsultationSession(
                        roomId,
                        consultation.getId(),
                        consultation.getRendezVous().getPatient().getId(),
                        consultation.getRendezVous().getMedecin().getId(),
                        consultation.getConsultationType()
                );

                activeSessions.put(roomId, session);

                // Clear waiting room
                waitingRooms.remove(roomId);

                // Notify patient
                TimelineEventDTO event = new TimelineEventDTO();
                event.setEventType("CONSULTATION_STARTED");
                event.setConsultationId(consultation.getId());
                event.setUserId(request.getDoctorId());
                event.setDescription("Consultation démarrée");

                messagingTemplate.convertAndSend(
                        "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                        event
                );

                // Notify doctor
                messagingTemplate.convertAndSend(
                        "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/consultation",
                        event
                );
            }
        }
    }

    @MessageMapping("/consultation.end")
    public void endConsultation(@Payload ConsultationEndRequestDTO request) {
        String roomId = request.getRoomId();
        ConsultationSession session = activeSessions.get(roomId);

        if (session != null) {
            Consultation consultation = consultationService.getConsultationById(session.getConsultationId());
            if (consultation != null) {
                // End consultation using service method
                consultation = consultationService.endConsultation(consultation.getId(), request.getNotes());

                if (consultation != null) {
                    // Update appointment status
                    RendezVous appointment = consultation.getRendezVous();
                    appointment.setStatus(StatusRendezVous.COMPLETED);
                    rendezVousService.updateRendezVous(appointment);

                    // Save consultation as medical record if requested
                    if (request.isSaveAsMedicalRecord()) {
                        saveConsultationAsMedicalRecord(consultation, request.getSummary());
                    }

                    // Generate prescription if requested
                    if (request.getPrescription() != null && !request.getPrescription().isEmpty()) {
                        generatePrescription(consultation, request.getPrescription());
                    }

                    // Remove active session
                    activeSessions.remove(roomId);

                    // Send end notification to both parties
                    TimelineEventDTO endEvent = new TimelineEventDTO();
                    endEvent.setEventType("CONSULTATION_ENDED");
                    endEvent.setConsultationId(consultation.getId());
                    endEvent.setUserId(request.getEndedBy());
                    endEvent.setDescription("Consultation terminée");

                    messagingTemplate.convertAndSend(
                            "/user/" + consultation.getRendezVous().getPatient().getId() + "/queue/consultation",
                            endEvent
                    );

                    messagingTemplate.convertAndSend(
                            "/user/" + consultation.getRendezVous().getMedecin().getId() + "/queue/consultation",
                            endEvent
                    );
                }
            }
        }
    }

    // Keep other WebSocket handlers as they are...
    @MessageMapping("/consultation.signal")
    public void handleSignal(@Payload SignalMessageDTO signal) {
        messagingTemplate.convertAndSend(
                "/user/" + signal.getTargetUserId() + "/queue/signal",
                signal
        );
    }

    @MessageMapping("/consultation.chat")
    public void sendConsultationChat(@Payload ChatMessageDTO message) {
        ConsultationSession session = activeSessions.get(message.getRoomId());
        if (session != null) {
            messagingTemplate.convertAndSend(
                    "/topic/consultation/" + message.getRoomId() + "/chat",
                    message
            );

            // Save chat message to consultation notes
            if (session.getChatHistory() == null) {
                session.setChatHistory(new ArrayList<>());
            }
            session.getChatHistory().add(message);
        }
    }

    private void saveConsultationAsMedicalRecord(Consultation consultation, String summary) {
        try {
            Patient patient = consultation.getRendezVous().getPatient();

            // Create a text file with consultation summary
            String fileName = "consultation_" + consultation.getId() + "_" +
                    consultation.getDate().getTime() + ".txt";
            String content = buildConsultationSummary(consultation, summary);

            // In a real implementation, you would save this as a file
            // and create a DossierMedical entry

            System.out.println("Consultation saved as medical record: " + fileName);

        } catch (Exception e) {
            System.err.println("Error saving consultation as medical record: " + e.getMessage());
        }
    }

    private void generatePrescription(Consultation consultation, List<String> medicaments) {
        Ordonance prescription = new Ordonance();
        prescription.setMedicaments(medicaments);
        prescription.setDateCreation(new java.util.Date());
        prescription.setValideeParIA(false);
        prescription.setConsultation(consultation);

        ordonanceService.createOrdonance(prescription);
    }

    private String buildConsultationSummary(Consultation consultation, String summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RÉSUMÉ DE CONSULTATION ===\n\n");
        sb.append("ID Consultation: ").append(consultation.getId()).append("\n");
        sb.append("Date: ").append(consultation.getDate()).append("\n");
        sb.append("Type: ").append(consultation.getConsultationType()).append("\n");
        sb.append("Médecin: ").append(consultation.getRendezVous().getMedecin().getName()).append("\n");
        sb.append("Patient: ").append(consultation.getRendezVous().getPatient().getName()).append("\n\n");
        sb.append("=== NOTES ===\n");
        sb.append(consultation.getNotes() != null ? consultation.getNotes() : "").append("\n\n");
        sb.append("=== RÉSUMÉ ===\n");
        sb.append(summary).append("\n");

        if (consultation.getOrdonance() != null && consultation.getOrdonance().getMedicaments() != null) {
            sb.append("\n=== ORDONNANCE ===\n");
            for (String medicament : consultation.getOrdonance().getMedicaments()) {
                sb.append("- ").append(medicament).append("\n");
            }
        }

        return sb.toString();
    }
}