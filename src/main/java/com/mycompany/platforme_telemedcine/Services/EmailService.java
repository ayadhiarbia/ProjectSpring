package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.User;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    // Simple console logging for development
    // In production, you can replace with real SMTP

    public void sendApprovalEmail(User user) {
        String subject = "Your TeleHealth Account Has Been Approved!";
        String body = """
            Dear %s %s,
            
            Your account with email %s has been approved by the administrator.
            
            You can now login to your account using your registered credentials.
            
            Login URL: http://localhost:8080/login
            
            Role: %s
            
            Best regards,
            TeleHealth+ Team
            """.formatted(user.getName(), user.getPrenom(), user.getEmail(), user.getRole());

        System.out.println("=========================================");
        System.out.println("APPROVAL EMAIL SENT TO: " + user.getEmail());
        System.out.println("=========================================");
        System.out.println(subject);
        System.out.println(body);
        System.out.println("=========================================");
    }

    public void sendRejectionEmail(User user, String reason) {
        String subject = "Your TeleHealth Account Application";
        String body = """
            Dear %s %s,
            
            We regret to inform you that your account application has been rejected.
            
            Reason: %s
            
            If you believe this is a mistake, please contact our support team.
            
            Best regards,
            TeleHealth+ Team
            """.formatted(user.getName(), user.getPrenom(), reason);

        System.out.println("=========================================");
        System.out.println("REJECTION EMAIL SENT TO: " + user.getEmail());
        System.out.println("=========================================");
        System.out.println(subject);
        System.out.println(body);
        System.out.println("=========================================");
    }

    public void sendRegistrationConfirmation(User user) {
        String subject = "TeleHealth+ - Registration Received";
        String body = """
            Dear %s %s,
            
            Thank you for registering with TeleHealth+!
            
            Your account is currently pending administrator approval.
            You will receive another email once your account is approved.
            
            Registration Details:
            - Name: %s %s
            - Email: %s
            - Role: %s
            - Status: Pending Approval
            
            You will not be able to login until your account is approved.
            
            Best regards,
            TeleHealth+ Team
            """.formatted(user.getName(), user.getPrenom(),
                user.getName(), user.getPrenom(),
                user.getEmail(), user.getRole());

        System.out.println("=========================================");
        System.out.println("REGISTRATION CONFIRMATION SENT TO: " + user.getEmail());
        System.out.println("=========================================");
        System.out.println(subject);
        System.out.println(body);
        System.out.println("=========================================");
    }
}