package com.example.health.mate;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.dto.SendOtpRequestDTO;
import com.example.health.mate.repository.OtpTokenRepository;
import com.example.health.mate.service.OtpTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"app.mail.host=", "app.mail.from="})
@ActiveProfiles("test")
class OtpMailUnavailableIntegrationTests {
    @Autowired OtpTokenService service;
    @Autowired OtpTokenRepository otps;

    @Test
    void refusesIssuanceWithoutSmtpConfigurationAndLeavesNoChallenge() {
        SendOtpRequestDTO request = new SendOtpRequestDTO();
        request.setEmail("unconfigured-" + UUID.randomUUID() + "@example.test");
        request.setPurpose(OtpPurpose.PASSWORD_RESET);
        assertThatThrownBy(() -> service.createOtp(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to send verification email. Please try again later.");
        assertThat(otps.findByEmail(request.getEmail())).isEmpty();
    }
}
