package com.mycompany.platforme_telemedcine.Repository;

import com.mycompany.platforme_telemedcine.Models.Messagerie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagerieRepository extends JpaRepository<Messagerie, Long> {

    @Query("SELECT m FROM Messagerie m WHERE " +
            "(m.senderId = :user1 AND m.receiverId = :user2) OR " +
            "(m.senderId = :user2 AND m.receiverId = :user1) " +
            "ORDER BY m.timestamp ASC")
    List<Messagerie> findConversationBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("SELECT m FROM Messagerie m WHERE m.senderId = :userId OR m.receiverId = :userId ORDER BY m.timestamp DESC")
    List<Messagerie> findByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM Messagerie m WHERE m.receiverId = :receiverId AND m.isRead = false")
    List<Messagerie> findUnreadMessagesByReceiverId(@Param("receiverId") Long receiverId);

    // Get unread messages for doctor from ALL patients
    @Query("SELECT m FROM Messagerie m WHERE m.receiverId = :doctorId AND m.senderRole = 'PATIENT' AND m.isRead = false")
    List<Messagerie> findUnreadMessagesForDoctor(@Param("doctorId") Long doctorId);

    // Get unread messages for doctor from SPECIFIC patient
    @Query("SELECT m FROM Messagerie m WHERE m.receiverId = :doctorId AND m.senderId = :patientId AND m.isRead = false")
    List<Messagerie> findUnreadMessagesForDoctorFromPatient(@Param("doctorId") Long doctorId,
                                                            @Param("patientId") Long patientId);

    // Get all unique patients who have conversations with this doctor
    @Query("SELECT DISTINCT m.senderId FROM Messagerie m WHERE m.receiverId = :doctorId AND m.senderRole = 'PATIENT'")
    List<Long> findDoctorPatientConversations(@Param("doctorId") Long doctorId);

    // Get last message from each patient for a doctor
    @Query("SELECT m FROM Messagerie m WHERE m.id IN (" +
            "SELECT MAX(m2.id) FROM Messagerie m2 " +
            "WHERE (m2.senderId = :doctorId AND m2.receiverId IN " +
            "(SELECT DISTINCT m3.senderId FROM Messagerie m3 WHERE m3.receiverId = :doctorId AND m3.senderRole = 'PATIENT')) " +
            "OR (m2.receiverId = :doctorId AND m2.senderId IN " +
            "(SELECT DISTINCT m3.senderId FROM Messagerie m3 WHERE m3.receiverId = :doctorId AND m3.senderRole = 'PATIENT')) " +
            "GROUP BY CASE " +
            "WHEN m2.senderId = :doctorId THEN m2.receiverId " +
            "ELSE m2.senderId " +
            "END)")
    List<Messagerie> findLastMessagesForDoctor(@Param("doctorId") Long doctorId);
}