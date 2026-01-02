package com.mycompany.platforme_telemedcine.config;
import com.mycompany.platforme_telemedcine.Controllers.NotificationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Autowired
    private NotificationController notificationController;

    // Run every day at 8 AM for appointment reminders
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyAppointmentReminders() {
        notificationController.scheduleDailyReminders();
    }

    // Check every minute for upcoming video consultations
    @Scheduled(cron = "0 * * * * *")
    public void checkUpcomingConsultations() {
        notificationController.checkUpcomingConsultations();
    }
}