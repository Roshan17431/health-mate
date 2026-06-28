package com.example.health.mate.service;

import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.AuthRequestDTO;
import com.example.health.mate.dto.AuthResponseDTO;
import com.example.health.mate.dto.RegisterRequestDTO;
import com.example.health.mate.dto.UserDTO;
import com.example.health.mate.entity.Patient;
import com.example.health.mate.entity.User;
import com.example.health.mate.exception.OurException;
import com.example.health.mate.repository.PatientRepository;
import com.example.health.mate.repository.UserRepository;
import com.example.health.mate.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PatientRepository patientRepository;

    public AuthService(
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            JwtService jwtService,
            PatientRepository patientRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.patientRepository = patientRepository;
    }


    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new OurException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole() == null ? USER_ROLE.ROLE_PATIENT : request.getRole());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());

        User savedUser = userRepository.save(user);

        if (savedUser.getRole() == USER_ROLE.ROLE_PATIENT) {
            ensurePatientProfile(savedUser);
        }

        String token = jwtService.generateToken(savedUser);
        return buildAuthResponse(token, savedUser);
    }


    public AuthResponseDTO login(AuthRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new OurException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new OurException("Invalid email or password"));

        if (user.getRole() == USER_ROLE.ROLE_PATIENT) {
            ensurePatientProfile(user);
        }

        String token = jwtService.generateToken(user);
        return buildAuthResponse(token, user);
    }

    private AuthResponseDTO buildAuthResponse(String token, User user) {
        return new AuthResponseDTO(token, TOKEN_TYPE, jwtService.getJwtExpirationMs(), toUserDto(user));
    }

    private void ensurePatientProfile(User user) {
        if (!patientRepository.existsByUserId(user.getId())) {
            Patient patient = new Patient();
            patient.setUser(user);
            patientRepository.save(patient);
        }
    }

    private UserDTO toUserDto(User user) {
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
