package com.example.health.mate.service;

import com.example.health.mate.dto.NotificationDTO;
import com.example.health.mate.dto.NotificationRequestDTO;
import com.example.health.mate.entity.Notification;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository, UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }


    public NotificationDTO createNotification(NotificationRequestDTO request) {
        User user = userService.getUserEntity(request.getUserId());
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(request.getMessage());
        notification.setIsRead(Boolean.FALSE);
        return toDto(notificationRepository.save(notification));
    }


    @Transactional(readOnly = true)
    public NotificationDTO getNotificationById(Long id) {
        return toDto(getNotificationEntity(id));
    }


    @Transactional(readOnly = true)
    public List<NotificationDTO> getNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByTimestampDesc(userId).stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public List<NotificationDTO> getUnreadNotificationsByUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByTimestampDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional(readOnly = true)
    public Long countUnreadNotifications(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }


    public NotificationDTO markAsRead(Long id) {
        Notification notification = getNotificationEntity(id);
        notification.setIsRead(Boolean.TRUE);
        return toDto(notificationRepository.save(notification));
    }


    public void deleteNotification(Long id) {
        notificationRepository.delete(getNotificationEntity(id));
    }


    @Transactional(readOnly = true)
    public Notification getNotificationEntity(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new OurException("Notification not found with id: " + id));
    }


    public NotificationDTO toDto(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationDTO(
                notification.getId(),
                notification.getUser() != null ? notification.getUser().getId() : null,
                notification.getMessage(),
                notification.getIsRead(),
                notification.getTimestamp()
        );
    }
}
