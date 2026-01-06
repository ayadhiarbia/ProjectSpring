package com.mycompany.platforme_telemedcine.Controllers;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.*;
import com.mycompany.platforme_telemedcine.Services.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    private final UserRepository userRepository;
    private final MedecinRepository medecinRepository;
    private final PatientRepository patientRepository;
    private final RendezVousRepository rendezVousRepository;
    private final PaiementRepository paiementRepository;
    private final EmailService emailService;

    @Autowired
    public AdminViewController(
            UserRepository userRepository,
            MedecinRepository medecinRepository,
            PatientRepository patientRepository,
            RendezVousRepository rendezVousRepository,
            PaiementRepository paiementRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.medecinRepository = medecinRepository;
        this.patientRepository = patientRepository;
        this.rendezVousRepository = rendezVousRepository;
        this.paiementRepository = paiementRepository;
        this.emailService = emailService;
    }

    /* ================= AUTH CHECK ================= */

    private User getAuthenticatedAdmin(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        // Get user from database
        User user = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return null;
        }

        // Check if user has ADMIN role
        if (!user.getRole().name().equals("ADMIN")) {
            return null;
        }

        return user;
    }

    private void setupSessionAndModel(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      HttpSession session, Model model) {
        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser != null) {
            session.setAttribute("user", adminUser);
            session.setAttribute("role", adminUser.getRole());
            model.addAttribute("user", adminUser);
        }
    }

    /* ================= DASHBOARD WITH CHART DATA ================= */

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            HttpSession session, Model model) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        // Store user in session for backward compatibility
        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        // Existing stats
        long totalUsers = userRepository.count();
        long totalDoctors = medecinRepository.count();
        long totalPatients = patientRepository.count();

        long pendingPatients = patientRepository.findAll()
                .stream()
                .filter(p -> p.getStatus() == UserStatus.PENDING)
                .count();

        long pendingDoctors = medecinRepository.findAll()
                .stream()
                .filter(d -> d.getStatus() == UserStatus.PENDING)
                .count();

        LocalDate today = LocalDate.now();
        long todayAppointments = rendezVousRepository.countByDate(today);

        LocalDate startOfMonth = today.withDayOfMonth(1);
        Double revenue = paiementRepository.sumByDateRange(
                java.sql.Date.valueOf(startOfMonth),
                java.sql.Date.valueOf(today)
        );

        // Chart Data: User Registration Trend (Last 7 Days)
        Map<String, Object> userTrendData = getUserRegistrationTrendData();
        model.addAttribute("userTrendData", userTrendData);

        // Chart Data: Appointment Distribution by Status
        Map<String, Object> appointmentDistribution = getAppointmentDistributionData();
        model.addAttribute("appointmentDistribution", appointmentDistribution);

        // Add stats to model
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("totalPatients", totalPatients);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("monthlyRevenue", revenue != null ? revenue : 0);
        model.addAttribute("pendingPatients", pendingPatients);
        model.addAttribute("pendingDoctors", pendingDoctors);
        model.addAttribute("totalPending", pendingPatients + pendingDoctors);
        model.addAttribute("user", adminUser);

        return "admin/dashboard";
    }

    /* ================= CHART DATA METHODS ================= */

    private Map<String, Object> getUserRegistrationTrendData() {
        Map<String, Object> chartData = new LinkedHashMap<>();

        // Sample data for the last 7 days
        List<String> labels = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        List<Long> userCounts = Arrays.asList(5L, 8L, 12L, 7L, 15L, 10L, 18L);
        List<Long> patientCounts = Arrays.asList(3L, 5L, 8L, 4L, 10L, 6L, 12L);
        List<Long> doctorCounts = Arrays.asList(2L, 3L, 4L, 3L, 5L, 4L, 6L);

        chartData.put("labels", labels);
        chartData.put("userCounts", userCounts);
        chartData.put("patientCounts", patientCounts);
        chartData.put("doctorCounts", doctorCounts);

        return chartData;
    }

    private Map<String, Object> getAppointmentDistributionData() {
        Map<String, Object> chartData = new LinkedHashMap<>();

        // Get appointment distribution by status using the repository method
        List<Object[]> statusCounts = rendezVousRepository.countAppointmentsByStatus();

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        List<String> colors = new ArrayList<>();

        Map<String, String> colorMap = new HashMap<>();
        colorMap.put("CONFIRMED", "#4cc9f0");
        colorMap.put("CONFIRME", "#4cc9f0");
        colorMap.put("PENDING", "#f72585");
        colorMap.put("EN_ATTENTE", "#f72585");
        colorMap.put("CANCELLED", "#ff9e00");
        colorMap.put("ANNULE", "#ff9e00");
        colorMap.put("COMPLETED", "#06d6a0");
        colorMap.put("TERMINE", "#06d6a0");

        for (Object[] row : statusCounts) {
            StatusRendezVous status = (StatusRendezVous) row[0];
            Long count = (Long) row[1];

            String statusName = status.toString();
            labels.add(statusName);
            data.add(count);
            colors.add(colorMap.getOrDefault(statusName.toUpperCase(), "#4361ee"));
        }

        if (labels.isEmpty()) {
            try {
                StatusRendezVous[] enumValues = StatusRendezVous.values();
                for (StatusRendezVous status : enumValues) {
                    String statusName = status.toString();
                    labels.add(statusName);
                    data.add(0L);
                    colors.add(colorMap.getOrDefault(statusName.toUpperCase(), "#4361ee"));
                }
            } catch (Exception e) {
                labels = Arrays.asList("CONFIRMED", "PENDING", "CANCELLED", "COMPLETED");
                data = Arrays.asList(45L, 15L, 10L, 30L);
                colors = Arrays.asList("#4cc9f0", "#f72585", "#ff9e00", "#06d6a0");
            }
        }

        chartData.put("labels", labels);
        chartData.put("data", data);
        chartData.put("colors", colors);

        return chartData;
    }

    /* ================= PENDING APPROVALS ================= */

    @GetMapping("/pending-approvals")
    public String pendingApprovals(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   HttpSession session, Model model) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        model.addAttribute("pendingPatients",
                patientRepository.findAll().stream()
                        .filter(p -> p.getStatus() == UserStatus.PENDING)
                        .toList()
        );

        model.addAttribute("pendingDoctors",
                medecinRepository.findAll().stream()
                        .filter(d -> d.getStatus() == UserStatus.PENDING)
                        .toList()
        );

        model.addAttribute("user", adminUser);
        return "admin/pending-approvals";
    }

    @PostMapping("/approve-user/{id}")
    public String approveUser(
            @PathVariable Long id,
            @RequestParam String userType,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes ra
    ) {
        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        if ("patient".equalsIgnoreCase(userType)) {
            patientRepository.findById(id).ifPresent(p -> {
                p.setStatus(UserStatus.APPROVED);
                p.setApprovedAt(LocalDateTime.now());
                patientRepository.save(p);
                emailService.sendApprovalEmail(p);
            });
        }

        if ("doctor".equalsIgnoreCase(userType)) {
            medecinRepository.findById(id).ifPresent(d -> {
                d.setStatus(UserStatus.APPROVED);
                d.setApprovedAt(LocalDateTime.now());
                medecinRepository.save(d);
                emailService.sendApprovalEmail(d);
            });
        }

        ra.addFlashAttribute("success", "User approved successfully");
        return "redirect:/admin/pending-approvals";
    }

    @PostMapping("/reject-user/{id}")
    public String rejectUser(
            @PathVariable Long id,
            @RequestParam String userType,
            @RequestParam(required = false) String reason,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            RedirectAttributes ra
    ) {
        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        if ("patient".equalsIgnoreCase(userType)) {
            patientRepository.findById(id).ifPresent(p -> {
                p.setStatus(UserStatus.REJECTED);
                p.setRejectionReason(reason);
                patientRepository.save(p);
                emailService.sendRejectionEmail(p, reason);
            });
        }

        if ("doctor".equalsIgnoreCase(userType)) {
            medecinRepository.findById(id).ifPresent(d -> {
                d.setStatus(UserStatus.REJECTED);
                d.setRejectionReason(reason);
                medecinRepository.save(d);
                emailService.sendRejectionEmail(d, reason);
            });
        }

        ra.addFlashAttribute("success", "User rejected successfully");
        return "redirect:/admin/pending-approvals";
    }

    /* ================= APPOINTMENTS ================= */

    @GetMapping("/appointments")
    public String appointments(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpSession session,
            Model model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        List<RendezVous> list = (date != null)
                ? rendezVousRepository.findByDate(date)
                : rendezVousRepository.findAll();

        if (status != null && !status.isBlank()) {
            try {
                StatusRendezVous s = StatusRendezVous.valueOf(status.toUpperCase());
                list = list.stream()
                        .filter(r -> r.getStatus() == s)
                        .toList();
            } catch (IllegalArgumentException e) {
                // If invalid status, return all appointments
            }
        }

        model.addAttribute("appointments", list);
        model.addAttribute("allStatuses", StatusRendezVous.values());
        model.addAttribute("user", adminUser);
        return "admin/appointments";
    }

    /* ================= PAYMENTS ================= */

    @GetMapping("/payments")
    public String payments(@AuthenticationPrincipal CustomUserDetails userDetails,
                           HttpSession session, Model model) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        List<Paiement> payments = paiementRepository.findAll();
        double total = payments.stream()
                .mapToDouble(p -> p.getMontant() != null ? p.getMontant() : 0)
                .sum();

        // Calculate counts
        long paidCount = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equals("PAID"))
                .count();

        long pendingCount = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equals("PENDING"))
                .count();

        long failedCount = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equals("FAILED"))
                .count();

        long refundedCount = payments.stream()
                .filter(p -> p.getStatus() != null && p.getStatus().equals("REFUNDED"))
                .count();

        model.addAttribute("payments", payments);
        model.addAttribute("totalAmount", total);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("refundedCount", refundedCount);
        model.addAttribute("user", adminUser);

        return "admin/payments";
    }

    /* ================= USERS MANAGEMENT ================= */

    @GetMapping("/users")
    public String usersManagement(@AuthenticationPrincipal CustomUserDetails userDetails,
                                  HttpSession session, Model model,
                                  @RequestParam(required = false) String role,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String search) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        // Get all users from different repositories
        List<User> adminUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN)
                .toList();

        List<Medecin> doctorUsers = medecinRepository.findAll();
        List<Patient> patientUsers = patientRepository.findAll();

        // Prepare combined user list
        List<Map<String, Object>> combinedUsers = new ArrayList<>();

        // Add users to combined list
        for (User user : adminUsers) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("name", user.getName() + " " + (user.getPrenom() != null ? user.getPrenom() : ""));
            userData.put("email", user.getEmail());
            userData.put("role", "ADMIN");
            userData.put("roleDisplay", "Administrator");
            userData.put("status", user.getStatus() != null ? user.getStatus().toString() : "PENDING");
            userData.put("statusDisplay", getStatusDisplayName(user.getStatus()));
            userData.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A");
            userData.put("approvedAt", "Not applicable");
            combinedUsers.add(userData);
        }

        for (Medecin doctor : doctorUsers) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", doctor.getId());
            userData.put("name", doctor.getName() + " " + (doctor.getPrenom() != null ? doctor.getPrenom() : ""));
            userData.put("email", doctor.getEmail());
            userData.put("role", "DOCTOR");
            userData.put("roleDisplay", "Doctor");
            userData.put("status", doctor.getStatus() != null ? doctor.getStatus().toString() : "PENDING");
            userData.put("statusDisplay", getStatusDisplayName(doctor.getStatus()));
            userData.put("createdAt", doctor.getCreatedAt() != null ? doctor.getCreatedAt().toString() : "N/A");
            userData.put("approvedAt", doctor.getApprovedAt() != null ? doctor.getApprovedAt().toString() : "Not approved");
            combinedUsers.add(userData);
        }

        for (Patient patient : patientUsers) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", patient.getId());
            userData.put("name", patient.getName() + " " + (patient.getPrenom() != null ? patient.getPrenom() : ""));
            userData.put("email", patient.getEmail());
            userData.put("role", "PATIENT");
            userData.put("roleDisplay", "Patient");
            userData.put("status", patient.getStatus() != null ? patient.getStatus().toString() : "PENDING");
            userData.put("statusDisplay", getStatusDisplayName(patient.getStatus()));
            userData.put("createdAt", patient.getCreatedAt() != null ? patient.getCreatedAt().toString() : "N/A");
            userData.put("approvedAt", patient.getApprovedAt() != null ? patient.getApprovedAt().toString() : "Not approved");
            combinedUsers.add(userData);
        }

        // Apply filters
        List<Map<String, Object>> filteredUsers = new ArrayList<>(combinedUsers);

        if (role != null && !role.isEmpty() && !role.equalsIgnoreCase("all")) {
            filteredUsers = filteredUsers.stream()
                    .filter(user -> user.get("role").toString().equalsIgnoreCase(role))
                    .toList();
        }

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            filteredUsers = filteredUsers.stream()
                    .filter(user -> user.get("status").toString().equalsIgnoreCase(status))
                    .toList();
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            filteredUsers = filteredUsers.stream()
                    .filter(user -> user.get("name").toString().toLowerCase().contains(searchLower) ||
                            user.get("email").toString().toLowerCase().contains(searchLower) ||
                            user.get("roleDisplay").toString().toLowerCase().contains(searchLower))
                    .toList();
        }

        // Sort by name
        filteredUsers.sort(Comparator.comparing(user -> user.get("name").toString()));

        // Calculate statistics
        long totalUsers = combinedUsers.size();
        long activeUsers = combinedUsers.stream()
                .filter(user -> user.get("status").toString().equals("ACTIVE") ||
                        user.get("status").toString().equals("APPROVED"))
                .count();
        long pendingUsers = combinedUsers.stream()
                .filter(user -> user.get("status").toString().equals("PENDING"))
                .count();
        long inactiveUsers = combinedUsers.stream()
                .filter(user -> user.get("status").toString().equals("INACTIVE") ||
                        user.get("status").toString().equals("REJECTED"))
                .count();

        // Add attributes to model
        model.addAttribute("users", filteredUsers);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("pendingUsers", pendingUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);
        model.addAttribute("user", adminUser);
        model.addAttribute("currentRole", role);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSearch", search);

        return "admin/users";
    }

    private String getStatusDisplayName(UserStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case PENDING -> "Pending";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            default -> status.toString();
        };
    }

    /* ================= DOCTORS MANAGEMENT ================= */

    @GetMapping("/doctors")
    public String doctorsManagement(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpSession session, Model model,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String specialty,
                                    @RequestParam(required = false) String search) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        List<Medecin> doctors = medecinRepository.findAll();

        // Apply filters
        List<Medecin> filteredDoctors = new ArrayList<>(doctors);

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                filteredDoctors = filteredDoctors.stream()
                        .filter(d -> d.getStatus() == userStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                // If invalid status, show all
            }
        }

        if (specialty != null && !specialty.isEmpty() && !specialty.equalsIgnoreCase("all")) {
            filteredDoctors = filteredDoctors.stream()
                    .filter(d -> d.getSpecialte() != null &&
                            d.getSpecialte().equalsIgnoreCase(specialty))
                    .toList();
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            filteredDoctors = filteredDoctors.stream()
                    .filter(d -> d.getName().toLowerCase().contains(searchLower) ||
                            (d.getPrenom() != null && d.getPrenom().toLowerCase().contains(searchLower)) ||
                            d.getEmail().toLowerCase().contains(searchLower) ||
                            (d.getSpecialte() != null && d.getSpecialte().toLowerCase().contains(searchLower)))
                    .toList();
        }

        // Calculate statistics
        long totalDoctors = doctors.size();
        long activeDoctors = doctors.stream()
                .filter(d -> d.getStatus() == UserStatus.APPROVED)
                .count();
        long pendingDoctors = doctors.stream()
                .filter(d -> d.getStatus() == UserStatus.PENDING)
                .count();

        // Get unique specialties
        List<String> specialties = doctors.stream()
                .map(Medecin::getSpecialte)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        model.addAttribute("doctors", filteredDoctors);
        model.addAttribute("totalDoctors", totalDoctors);
        model.addAttribute("activeDoctors", activeDoctors);
        model.addAttribute("pendingDoctors", pendingDoctors);
        model.addAttribute("specialties", specialties);
        model.addAttribute("user", adminUser);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSpecialty", specialty);
        model.addAttribute("currentSearch", search);

        return "admin/doctors";
    }

    /* ================= PATIENTS MANAGEMENT ================= */

    @GetMapping("/patients")
    public String patientsManagement(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     HttpSession session, Model model,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String search) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        List<Patient> patients = patientRepository.findAll();

        // Apply filters
        List<Patient> filteredPatients = new ArrayList<>(patients);

        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
                filteredPatients = filteredPatients.stream()
                        .filter(p -> p.getStatus() == userStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                // If invalid status, show all
            }
        }

        if (search != null && !search.isEmpty()) {
            String searchLower = search.toLowerCase();
            filteredPatients = filteredPatients.stream()
                    .filter(p -> p.getName().toLowerCase().contains(searchLower) ||
                            (p.getPrenom() != null && p.getPrenom().toLowerCase().contains(searchLower)) ||
                            p.getEmail().toLowerCase().contains(searchLower))
                    .toList();
        }

        // Calculate statistics
        long totalPatients = patients.size();
        long activePatients = patients.stream()
                .filter(p -> p.getStatus() == UserStatus.APPROVED)
                .count();
        long pendingPatients = patients.stream()
                .filter(p -> p.getStatus() == UserStatus.PENDING)
                .count();

        model.addAttribute("patients", filteredPatients);
        model.addAttribute("totalPatients", totalPatients);
        model.addAttribute("activePatients", activePatients);
        model.addAttribute("pendingPatients", pendingPatients);
        model.addAttribute("user", adminUser);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSearch", search);

        return "admin/patients";
    }

    /* ================= REPORTS ================= */

    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal CustomUserDetails userDetails,
                          HttpSession session, Model model,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        User adminUser = getAuthenticatedAdmin(userDetails);
        if (adminUser == null) {
            return "redirect:/login";
        }

        session.setAttribute("user", adminUser);
        session.setAttribute("role", adminUser.getRole());

        // Set default date range (last 30 days)
        LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();
        LocalDate finalStartDate = startDate != null ? startDate : finalEndDate.minusDays(30);

        // Get user registration report
        long newUsers = userRepository.count();
        long newDoctors = medecinRepository.findAll().stream()
                .filter(d -> d.getCreatedAt() != null &&
                        d.getCreatedAt().toLocalDate().isAfter(finalStartDate.minusDays(1)) &&
                        d.getCreatedAt().toLocalDate().isBefore(finalEndDate.plusDays(1)))
                .count();
        long newPatients = patientRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null &&
                        p.getCreatedAt().toLocalDate().isAfter(finalStartDate.minusDays(1)) &&
                        p.getCreatedAt().toLocalDate().isBefore(finalEndDate.plusDays(1)))
                .count();

        // Get appointment statistics
        List<RendezVous> appointmentsInRange = rendezVousRepository.findByDateRange(finalStartDate, finalEndDate);
        long totalAppointments = appointmentsInRange.size();
        long completedAppointments = 0;
        long cancelledAppointments = 0;

        try {
            StatusRendezVous completedStatus = StatusRendezVous.valueOf("COMPLETED");
            completedAppointments = appointmentsInRange.stream()
                    .filter(r -> r.getStatus() == completedStatus)
                    .count();
        } catch (IllegalArgumentException e) {
            // COMPLETED doesn't exist in StatusRendezVous enum
        }

        try {
            StatusRendezVous cancelledStatus = StatusRendezVous.valueOf("CANCELLED");
            cancelledAppointments = appointmentsInRange.stream()
                    .filter(r -> r.getStatus() == cancelledStatus)
                    .count();
        } catch (IllegalArgumentException e) {
            // CANCELLED doesn't exist in StatusRendezVous enum
        }

        // Get revenue statistics
        Double totalRevenue = paiementRepository.sumByDateRange(
                java.sql.Date.valueOf(finalStartDate),
                java.sql.Date.valueOf(finalEndDate)
        );

        // Get monthly revenue trend (last 6 months)
        List<Map<String, Object>> revenueTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = finalEndDate.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            Double monthlyRevenue = paiementRepository.sumByDateRange(
                    java.sql.Date.valueOf(monthStart),
                    java.sql.Date.valueOf(monthEnd)
            );

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            monthData.put("revenue", monthlyRevenue != null ? monthlyRevenue : 0.0);
            monthData.put("appointments", rendezVousRepository.findByDateRange(monthStart, monthEnd).size());
            revenueTrend.add(monthData);
        }

        model.addAttribute("startDate", finalStartDate);
        model.addAttribute("endDate", finalEndDate);
        model.addAttribute("newUsers", newUsers);
        model.addAttribute("newDoctors", newDoctors);
        model.addAttribute("newPatients", newPatients);
        model.addAttribute("totalAppointments", totalAppointments);
        model.addAttribute("completedAppointments", completedAppointments);
        model.addAttribute("cancelledAppointments", cancelledAppointments);
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        model.addAttribute("revenueTrend", revenueTrend);
        model.addAttribute("user", adminUser);

        return "admin/reports";
    }
}