package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Notification;
import com.mycompany.platforme_telemedcine.Repository.NotificationRepository;
import com.mycompany.platforme_telemedcine.Services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImp implements NotificationService { // Add "implements NotificationService"

    @Autowired
    private NotificationRepository notificationRepository;

    @Override // Add this annotation
    public Notification createNotification(String message, Long medecinId) {
        Notification notification = new Notification(message, medecinId);
        return notificationRepository.save(notification);
    }

    @Override // Add this annotation
    public List<Notification> getNotificationsByMedecinId(Long medecinId) {
        return notificationRepository.findByMedecinIdOrderByCreatedAtDesc(medecinId);
    }

    @Override // Add this annotation
    public List<Notification> getUnreadNotificationsByMedecinId(Long medecinId) {
        return notificationRepository.findByMedecinIdAndReadFalseOrderByCreatedAtDesc(medecinId);
    }

    @Override // Add this annotation
    public Notification markAsRead(Long notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        if (notification.isPresent()) {
            Notification notif = notification.get();
            notif.setRead(true);
            return notificationRepository.save(notif);
        }
        return null;
    }

    @Override // Add this annotation
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    @Override // Add this annotation
    public void deleteAllNotificationsByMedecinId(Long medecinId) {
        notificationRepository.deleteByMedecinId(medecinId);
    }

    @Override // Add this annotation
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }
}