package com.mycompany.platforme_telemedcine.RestControllers;

import com.mycompany.platforme_telemedcine.Models.Notification;
import com.mycompany.platforme_telemedcine.Services.ImpService.NotificationServiceImp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationRestController {

    @Autowired
    private NotificationServiceImp notificationService;

    @GetMapping("/medecin/{medecinId}")
    public ResponseEntity<List<Notification>> getNotificationsByMedecin(@PathVariable Long medecinId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsByMedecinId(medecinId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/medecin/{medecinId}/unread")
    public ResponseEntity<List<Notification>> getUnreadNotificationsByMedecin(@PathVariable Long medecinId) {
        try {
            List<Notification> notifications = notificationService.getUnreadNotificationsByMedecinId(medecinId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            if (notification != null) {
                return ResponseEntity.ok(notification);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HttpStatus> deleteNotification(@PathVariable Long id) {
        try {
            notificationService.deleteNotification(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/medecin/{medecinId}/all")
    public ResponseEntity<HttpStatus> deleteAllNotificationsByMedecin(@PathVariable Long medecinId) {
        try {
            notificationService.deleteAllNotificationsByMedecinId(medecinId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

