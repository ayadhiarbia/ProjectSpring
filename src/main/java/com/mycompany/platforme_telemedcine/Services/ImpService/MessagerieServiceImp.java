package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Messagerie;
import com.mycompany.platforme_telemedcine.Repository.MessagerieRepository;
import com.mycompany.platforme_telemedcine.Services.MessagerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MessagerieServiceImp implements MessagerieService {

    @Autowired
    private MessagerieRepository messagerieRepository;

    @Override
    public List<Messagerie> getMessagesBetween(Long userId1, Long userId2) {
        return messagerieRepository.findConversationBetweenUsers(userId1, userId2);
    }

    @Override
    public Messagerie saveMessage(Messagerie message) {
        return messagerieRepository.save(message);
    }

    @Override
    public List<Messagerie> getUserMessages(Long userId) {
        return messagerieRepository.findByUserId(userId);
    }

    @Override
    public List<Messagerie> getConversationHistory(Long userId1, Long userId2) {
        return messagerieRepository.findConversationBetweenUsers(userId1, userId2);
    }

    @Override
    public void markMessagesAsRead(Long senderId, Long receiverId) {
        List<Messagerie> messages = messagerieRepository.findConversationBetweenUsers(senderId, receiverId);
        for (Messagerie message : messages) {
            if (message.getReceiverId().equals(receiverId)) {
                message.setRead(true); // Assuming you have this field in Messagerie model
                messagerieRepository.save(message);
            }
        }
    }

    @Override
    public List<Messagerie> getUnreadMessages(Long receiverId) {
        return messagerieRepository.findUnreadMessagesByReceiverId(receiverId);
    }

    @Override
    public List<Messagerie> getUnreadMessagesForDoctor(Long doctorId, Long patientId) {
        return messagerieRepository.findUnreadMessagesForDoctor(doctorId, patientId);
    }
}
