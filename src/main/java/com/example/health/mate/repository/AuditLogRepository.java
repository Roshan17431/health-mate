package com.example.health.mate.repository;

import com.example.health.mate.entity.AuditLog;
import com.example.health.mate.entity.TimeSlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<TimeSlot, Long> {

    Page<AuditLog> findByUserIdOrderByTimetampDesc(Integer userId, Pageable pageable);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
