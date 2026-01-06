package com.mycompany.platforme_telemedcine.Services.ImpService;

import com.mycompany.platforme_telemedcine.Models.Messagerie;
import com.mycompany.platforme_telemedcine.Repository.MessagerieRepository;
import com.mycompany.platforme_telemedcine.Services.MessagerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void markMessagesAsRead(Long senderId, Long receiverId) {
        List<Messagerie> messages = messagerieRepository.findConversationBetweenUsers(senderId, receiverId);
        for (Messagerie message : messages) {
            if (message.getReceiverId().equals(receiverId) && !message.isRead()) {
                message.setRead(true);
                messagerieRepository.save(message);
            }
        }
    }

    @Override
    public List<Messagerie> getUnreadMessages(Long receiverId) {
        return messagerieRepository.findUnreadMessagesByReceiverId(receiverId);
    }

    @Override
    public List<Messagerie> getUnreadMessagesForDoctor(Long doctorId) {
        return messagerieRepository.findUnreadMessagesForDoctor(doctorId);
    }

    @Override
    public List<Messagerie> getUnreadMessagesForDoctorFromPatient(Long doctorId, Long patientId) {
        return messagerieRepository.findUnreadMessagesForDoctorFromPatient(doctorId, patientId);
    }

    @Override
    public List<Messagerie> getDoctorConversations(Long doctorId) {
        return messagerieRepository.findLastMessagesForDoctor(doctorId);
    }

    @Override
    @Transactional
    public void markDoctorMessagesAsRead(Long doctorId, Long patientId) {
        List<Messagerie> messages = messagerieRepository.findUnreadMessagesForDoctorFromPatient(doctorId, patientId);
        for (Messagerie message : messages) {
            message.setRead(true);
            messagerieRepository.save(message);
        }
    }

    @Override
    public List<Long> getDoctorConversationPartners(Long doctorId) {
        return messagerieRepository.findDoctorPatientConversations(doctorId);
    }
}