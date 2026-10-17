package com.example.health.mate.repository;

import com.example.health.mate.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {

    @Modifying
    @Query("delete from Notification n where n.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    List<Notification> findByUserIdOrderByTimestampDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByTimestampDesc(Long userId);

    Long countByUserIdAndIsReadFalse(Long userId);
}
