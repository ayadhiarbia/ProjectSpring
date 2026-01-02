package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.Notification;
import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification createNotification(String message, Long medecinId);
    List<Notification> getNotificationsByMedecinId(Long medecinId);
    List<Notification> getUnreadNotificationsByMedecinId(Long medecinId);
    Notification markAsRead(Long notificationId);
    void deleteNotification(Long notificationId);
    void deleteAllNotificationsByMedecinId(Long medecinId);
    Optional<Notification> getNotificationById(Long id);
}