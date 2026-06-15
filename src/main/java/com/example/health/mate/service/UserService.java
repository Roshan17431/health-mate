package com.example.health.mate.service;

import com.example.health.mate.dto.UserDTO;
import com.example.health.mate.dto.UserUpdateDTO;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for user profile reads and updates.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns every user as an API-safe DTO.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Returns one user by identifier.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return toDto(getUserEntity(id));
    }

    /**
     * Returns one user by email.
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        return toDto(userRepository.findByEmail(email)
                .orElseThrow(() -> new OurException("User not found with email: " + email)));
    }

    /**
     * Applies profile changes to a user.
     */
    public UserDTO updateUser(Long id, UserUpdateDTO request) {
        User user = getUserEntity(id);
        if (request.getName() != null) user.setName(request.getName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        return toDto(userRepository.save(user));
    }

    /**
     * Deletes a user profile.
     */
    public void deleteUser(Long id) {
        User user = getUserEntity(id);
        userRepository.delete(user);
    }

    /**
     * Loads a user entity for internal service composition.
     */
    @Transactional(readOnly = true)
    public User getUserEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new OurException("User not found with id: " + id));
    }

    /**
     * Converts a user entity to an API-safe DTO without password data.
     */
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
