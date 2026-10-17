package com.example.health.mate.service;

import com.example.health.mate.dto.UserDTO;
import com.example.health.mate.dto.UserUpdateDTO;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.UserRepository;
import com.example.health.mate.repository.AuditLogRepository;
import com.example.health.mate.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ProfileDeletionService profileDeletion;
    private final NotificationRepository notifications;
    private final AuditLogRepository audits;

    public UserService(UserRepository userRepository, ProfileDeletionService profileDeletion,
                       NotificationRepository notifications, AuditLogRepository audits) {
        this.userRepository = userRepository;
        this.profileDeletion = profileDeletion;
        this.notifications = notifications;
        this.audits = audits;
    }


    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }


    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return toDto(getUserEntity(id));
    }


    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        return toDto(userRepository.findByEmail(email)
                .orElseThrow(() -> new OurException("User not found with email: " + email)));
    }


    public UserDTO updateUser(Long id, UserUpdateDTO request) {
        User user = getUserEntity(id);
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        return toDto(userRepository.save(user));
    }


    public void deleteUser(Long id) {
        User user = getUserEntity(id);
        profileDeletion.deleteProfilesForUser(id);
        notifications.deleteByUserId(id);
        audits.unlinkUser(id);
        userRepository.delete(user);
    }


    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new OurException("User not found with id: " + id));
    }


    public UserDTO toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getCreatedDate(),
                user.getUpdatedDate()
        );
    }
}
