package com.mycompany.platforme_telemedcine.Services;

import com.mycompany.platforme_telemedcine.Models.Messagerie;
import java.util.List;

public interface MessagerieService {
    List<Messagerie> getMessagesBetween(Long userId1, Long userId2);
    Messagerie saveMessage(Messagerie message);
    List<Messagerie> getUserMessages(Long userId);
    List<Messagerie> getConversationHistory(Long userId1, Long userId2);
    void markMessagesAsRead(Long senderId, Long receiverId);
    List<Messagerie> getUnreadMessages(Long receiverId);

    // Doctor-specific methods - CORRECTED
    List<Messagerie> getUnreadMessagesForDoctor(Long doctorId);
    List<Messagerie> getUnreadMessagesForDoctorFromPatient(Long doctorId, Long patientId);
    List<Messagerie> getDoctorConversations(Long doctorId);
    void markDoctorMessagesAsRead(Long doctorId, Long patientId);
    List<Long> getDoctorConversationPartners(Long doctorId);
}