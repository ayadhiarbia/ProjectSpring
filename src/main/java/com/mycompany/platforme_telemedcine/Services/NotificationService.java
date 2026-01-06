package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.Notification;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface NotificationService {
    // Existing methods
    Notification createNotification(String message, Long medecinId);
    List<Notification> getNotificationsByMedecinId(Long medecinId);
    List<Notification> getUnreadNotificationsByMedecinId(Long medecinId);
    Notification markAsRead(Long notificationId);
    void deleteNotification(Long notificationId);
    void deleteAllNotificationsByMedecinId(Long medecinId);
    Optional<Notification> getNotificationById(Long id);

    // NEW METHODS FOR CONSULTATION WORKFLOW

    // Create notification with title and link
    Notification createNotification(Long userId, String title, String message, String link);

    // Create notification for patient
    Notification createNotificationForPatient(Long patientId, String title, String message, String link);

    // Create notification for doctor about consultation request
    Notification createConsultationRequestNotification(Long doctorId, Long patientId,
                                                       Long consultationId, String consultationType);

    // Create notification for patient about consultation approval

    Notification createConsultationApprovedNotification(Long patientId, Long doctorId,
                                                        Long consultationId, Date scheduledDate);

    // Create notification for patient about consultation rejection
    Notification createConsultationRejectedNotification(Long patientId, Long doctorId,
                                                        Long consultationId, String reason);

    // Create notification for doctor about consultation cancellation
    Notification createConsultationCancelledNotification(Long doctorId, Long patientId,
                                                         Long consultationId, String reason);

    // Create notification for doctor about consultation rescheduling
    Notification createConsultationRescheduledNotification(Long doctorId, Long patientId,
                                                           Long consultationId, Date newDate);

    // Create notification for consultation starting soon (reminder)
    Notification createConsultationReminderNotification(Long userId, Long consultationId,
                                                        Date scheduledDate, int minutesBefore);

    // Create notification for consultation starting now
    Notification createConsultationStartNotification(Long userId, Long consultationId);

    // Create notification for consultation completed
    Notification createConsultationCompletedNotification(Long userId, Long consultationId);

    // Create notification for new prescription
    Notification createPrescriptionNotification(Long patientId, Long doctorId,
                                                Long prescriptionId);

    // Create notification for new document upload
    Notification createDocumentUploadNotification(Long doctorId, Long patientId,
                                                  String documentName);

    // Mark all notifications as read for a user
    void markAllAsRead(Long userId);

    // Get notifications for a user (works for both patient and doctor)
    List<Notification> getNotificationsByUserId(Long userId);

    // Get unread notifications count for a user
    int getUnreadCount(Long userId);

    // Clear old notifications (older than X days)
    void clearOldNotifications(int days);
}