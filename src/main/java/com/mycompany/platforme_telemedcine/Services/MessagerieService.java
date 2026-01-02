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

    List<Messagerie> getUnreadMessagesForDoctor(Long id, Long id1);
}