package com.example.health.mate.repository;

import com.example.health.mate.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification,Long> {

    List<Notification> findByUserIdOrderByTimestampDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByTimestampDesc(Long userId);

    Long countByUserIdAndIsReadFalse(Long userId);
}
