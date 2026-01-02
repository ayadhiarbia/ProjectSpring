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

    // Add these methods:
    @Query("SELECT m FROM Messagerie m WHERE m.receiverId = :receiverId AND m.isRead = false")
    List<Messagerie> findUnreadMessagesByReceiverId(@Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Messagerie m WHERE m.receiverId = :doctorId AND m.senderId = :patientId AND m.isRead = false")
    List<Messagerie> findUnreadMessagesForDoctor(@Param("doctorId") Long doctorId, @Param("patientId") Long patientId);
}