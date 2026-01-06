package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.*;
import com.mycompany.platforme_telemedcine.Repository.NotificationRepository;
import com.mycompany.platforme_telemedcine.Services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImp implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationServiceImp(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public Notification createNotification(String message, Long medecinId) {
        Notification notification = new Notification();
        notification.setMessage(message);
        notification.setUserId(medecinId);
        notification.setCreatedAt(new Date());
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    @Override
    public Notification createNotification(Long userId, String title, String message, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setCreatedAt(new Date());
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    @Override
    public Notification createNotificationForPatient(Long patientId, String title, String message, String link) {
        return createNotification(patientId, title, message, link);
    }

    @Override
    public Notification createConsultationRequestNotification(Long doctorId, Long patientId,
                                                              Long consultationId, String consultationType) {
        String title = "Nouvelle demande de consultation";
        String message = "Le patient #" + patientId + " a demandé une consultation " + consultationType;
        String link = "/doctor/consultations/pending/" + consultationId;
        return createNotification(doctorId, title, message, link);
    }

    @Override
    public Notification createConsultationApprovedNotification(Long patientId, Long doctorId,
                                                               Long consultationId, Date scheduledDate) {
        String title = "Consultation approuvée";
        String message = "Votre consultation avec le docteur #" + doctorId + " a été approuvée pour le " +
                scheduledDate.toString();
        String link = "/patient/consultation/" + consultationId;
        return createNotification(patientId, title, message, link);
    }

    @Override
    public Notification createConsultationRejectedNotification(Long patientId, Long doctorId,
                                                               Long consultationId, String reason) {
        String title = "Consultation rejetée";
        String message = "Votre consultation avec le docteur #" + doctorId + " a été rejetée. Raison: " + reason;
        String link = "/patient/consultations";
        return createNotification(patientId, title, message, link);
    }

    @Override
    public Notification createConsultationCancelledNotification(Long doctorId, Long patientId,
                                                                Long consultationId, String reason) {
        String title = "Consultation annulée";
        String message = "Le patient #" + patientId + " a annulé la consultation. Raison: " + reason;
        String link = "/doctor/consultations";
        return createNotification(doctorId, title, message, link);
    }

    @Override
    public Notification createConsultationRescheduledNotification(Long doctorId, Long patientId,
                                                                  Long consultationId, Date newDate) {
        String title = "Consultation reprogrammée";
        String message = "Le patient #" + patientId + " a demandé à reprogrammer la consultation pour le " +
                newDate.toString();
        String link = "/doctor/consultations/pending/" + consultationId;
        return createNotification(doctorId, title, message, link);
    }

    @Override
    public Notification createConsultationReminderNotification(Long userId, Long consultationId,
                                                               Date scheduledDate, int minutesBefore) {
        String title = "Rappel de consultation";
        String message = "Votre consultation commence dans " + minutesBefore + " minutes";
        String link = "/consultation/room/" + consultationId;
        return createNotification(userId, title, message, link);
    }

    @Override
    public Notification createConsultationStartNotification(Long userId, Long consultationId) {
        String title = "Consultation commencée";
        String message = "Votre consultation est prête à démarrer";
        String link = "/consultation/room/" + consultationId;
        return createNotification(userId, title, message, link);
    }

    @Override
    public Notification createConsultationCompletedNotification(Long userId, Long consultationId) {
        String title = "Consultation terminée";
        String message = "Votre consultation est terminée. Consultez le résumé";
        String link = "/consultation/summary/" + consultationId;
        return createNotification(userId, title, message, link);
    }

    @Override
    public Notification createPrescriptionNotification(Long patientId, Long doctorId, Long prescriptionId) {
        String title = "Nouvelle ordonnance";
        String message = "Le docteur #" + doctorId + " vous a prescrit une ordonnance";
        String link = "/patient/prescriptions/" + prescriptionId;
        return createNotification(patientId, title, message, link);
    }

    @Override
    public Notification createDocumentUploadNotification(Long doctorId, Long patientId, String documentName) {
        String title = "Nouveau document";
        String message = "Le patient #" + patientId + " a uploadé le document: " + documentName;
        String link = "/doctor/documents";
        return createNotification(doctorId, title, message, link);
    }

    @Override
    public List<Notification> getNotificationsByMedecinId(Long medecinId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(medecinId);
    }

    @Override
    public List<Notification> getUnreadNotificationsByMedecinId(Long medecinId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(medecinId);
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public Notification markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return null;
    }

    @Override
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userId);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Override
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override
    public void deleteAllNotificationsByMedecinId(Long medecinId) {
        List<Notification> notifications = notificationRepository.findByUserId(medecinId);
        notificationRepository.deleteAll(notifications);
    }

    @Override
    public void clearOldNotifications(int days) {
        Date cutoffDate = new Date(System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000));
        List<Notification> oldNotifications = notificationRepository.findByCreatedAtBefore(cutoffDate);
        notificationRepository.deleteAll(oldNotifications);
    }

    @Override
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }
}