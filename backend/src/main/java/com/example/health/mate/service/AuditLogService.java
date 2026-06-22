package com.example.health.mate.service;

import com.example.health.mate.dto.AuditLogDTO;
import com.example.health.mate.dto.AuditLogRequestDTO;
import com.example.health.mate.entity.AuditLog;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserService userService;

    public AuditLogService(AuditLogRepository auditLogRepository, UserService userService) {
        this.auditLogRepository = auditLogRepository;
        this.userService = userService;
    }


    public AuditLogDTO createAuditLog(AuditLogRequestDTO request) {
        User user = request.getUserId() == null ? null : userService.getUserEntity(request.getUserId());
        return toDto(saveAuditLog(user, request.getAction(), request.getDetails()));
    }


    public AuditLogDTO logAction(Long userId, String action, String details) {
        User user = userId == null ? null : userService.getUserEntity(userId);
        return toDto(saveAuditLog(user, action, details));
    }


    @Transactional(readOnly = true)
    public AuditLogDTO getAuditLogById(Long id) {
        return toDto(getAuditLogEntity(id));
    }


    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable).map(this::toDto);
    }


    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end).stream().map(this::toDto).toList();
    }


    public void deleteAuditLog(Long id) {
        auditLogRepository.delete(getAuditLogEntity(id));
    }


    @Transactional(readOnly = true)
    public AuditLog getAuditLogEntity(Long id) {
        return auditLogRepository.findById(id)
                .orElseThrow(() -> new OurException("Audit log not found with id: " + id));
    }


    public AuditLogDTO toDto(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        return new AuditLogDTO(
                auditLog.getId(),
                auditLog.getUser() != null ? auditLog.getUser().getId() : null,
                auditLog.getAction(),
                auditLog.getDetails(),
                auditLog.getTimestamp()
        );
    }

    private AuditLog saveAuditLog(User user, String action, String details) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        return auditLogRepository.save(auditLog);
    }
}
