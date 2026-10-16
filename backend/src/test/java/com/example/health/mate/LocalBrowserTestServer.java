package com.example.health.mate;

import com.example.health.mate.domain.GENDER;
import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.domain.USER_ROLE;
import com.example.health.mate.dto.DoctorRequestDTO;
import com.example.health.mate.dto.RegisterRequestDTO;
import com.example.health.mate.repository.OtpTokenRepository;
import com.example.health.mate.service.AuthService;
import com.example.health.mate.service.DoctorService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;
import java.util.Scanner;

/** Manual Playwright fixture server. Hard-coded H2 settings keep browser writes out of PostgreSQL. */
public class LocalBrowserTestServer {
    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = SpringApplication.run(HealthMateApplication.class,
                "--spring.profiles.active=test", "--server.port=8081",
                "--spring.datasource.url=jdbc:h2:mem:healthmate_browser;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.username=sa", "--spring.datasource.password=",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--spring.sql.init.mode=never",
                "--app.cors.allowed-origins=http://localhost:3001,http://localhost:3000")) {
            AuthService auth = context.getBean(AuthService.class);
            for (USER_ROLE role : USER_ROLE.values()) {
                String name = role.name().substring(5).toLowerCase();
                RegisterRequestDTO request = new RegisterRequestDTO();
                request.setName("Browser " + name);
                request.setEmail(name + "@example.test");
                request.setPassword("Local-test-123!");
                request.setPhoneNumber("browser-" + name);
                request.setRole(role);
                request.setGender(GENDER.OTHER);
                request.setDateOfBirth(LocalDate.of(1990, 1, 1));
                request.setAddress("Disposable browser test data");
                Long userId = auth.register(request).getUser().getId();
                if (role == USER_ROLE.ROLE_DOCTOR) {
                    DoctorRequestDTO doctor = new DoctorRequestDTO();
                    doctor.setUserId(userId);
                    doctor.setExperience(5);
                    doctor.setSpecialization("Cardiology");
                    doctor.setAvailableHours("09:00–17:00");
                    context.getBean(DoctorService.class).createDoctor(doctor);
                }
            }
            System.out.println("BROWSER_FIXTURES_READY: port 8081; stdin: otp <email> or quit");
            Scanner input = new Scanner(System.in);
            while (input.hasNextLine()) {
                String command = input.nextLine().trim();
                if (command.equals("quit")) break;
                if (command.startsWith("otp ")) {
                    context.getBean(OtpTokenRepository.class)
                            .findTopByEmailAndPurposeOrderByCreatedAtDesc(command.substring(4), OtpPurpose.PASSWORD_RESET)
                            .ifPresent(otp -> System.out.println("BROWSER_FIXTURE_OTP: " + otp.getOtpCode()));
                }
            }
        }
    }
}
