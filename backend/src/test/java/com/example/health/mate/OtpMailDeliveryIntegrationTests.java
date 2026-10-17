package com.example.health.mate;

import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.dto.SendOtpRequestDTO;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.repository.OtpTokenRepository;
import com.example.health.mate.service.OtpTokenService;
import com.example.health.mate.support.LocalSmtpServer;
import com.example.health.mate.support.TestSmtpInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestSmtpInitializer.class)
class OtpMailDeliveryIntegrationTests {
    @Autowired OtpTokenService service;
    @Autowired OtpTokenRepository otps;
    @Autowired LocalSmtpServer smtp;

    @Test
    void deliversTheStoredCodeToTheRequestedRecipientOverSmtp() throws Exception {
        SendOtpRequestDTO request = request();
        service.createOtp(request);
        OtpToken stored = otps.findTopByEmailAndPurposeOrderByCreatedAtDesc(request.getEmail(), request.getPurpose()).orElseThrow();
        String email = smtp.awaitMessage(Duration.ofSeconds(1));
        assertThat(email).as("SMTP message accepted by the loopback server").isNotNull();
        assertThat(email).contains("RCPT TO:<" + request.getEmail() + ">", "From: healthmate@example.test",
                "Subject: Health Mate password reset code", stored.getOtpCode(), "10 minutes");
        assertThat(stored.getOtpCode()).matches("\\d{6}");
    }

    @Test
    void rollsBackTheChallengeWhenTheSmtpServerRejectsDelivery() {
        SendOtpRequestDTO request = request();
        smtp.rejectMessages(true);
        try {
            assertThatThrownBy(() -> service.createOtp(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Unable to send verification email. Please try again later.");
            assertThat(otps.findByEmail(request.getEmail())).isEmpty();
        } finally {
            smtp.rejectMessages(false);
        }
    }

    private SendOtpRequestDTO request() {
        SendOtpRequestDTO request = new SendOtpRequestDTO();
        request.setEmail("smtp-" + UUID.randomUUID() + "@example.test");
        request.setPurpose(OtpPurpose.PASSWORD_RESET);
        return request;
    }
}
