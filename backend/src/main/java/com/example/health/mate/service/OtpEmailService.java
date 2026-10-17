package com.example.health.mate.service;

import com.example.health.mate.config.MailSettings;
import com.example.health.mate.domain.OtpPurpose;
import com.example.health.mate.entity.OtpToken;
import com.example.health.mate.exception.OtpDeliveryException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OtpEmailService {
    private final JavaMailSender sender;
    private final MailSettings settings;

    public OtpEmailService(JavaMailSender sender, MailSettings settings) {
        this.sender = sender;
        this.settings = settings;
    }

    public void send(OtpToken otp) {
        if (!StringUtils.hasText(settings.getHost()) || !StringUtils.hasText(settings.getFrom())
                || (settings.isAuth() && (!StringUtils.hasText(settings.getUsername())
                || !StringUtils.hasText(settings.getPassword())))) {
            throw new OtpDeliveryException();
        }
        String purpose = otp.getPurpose() == OtpPurpose.PASSWORD_RESET ? "password reset" : "password change";
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(settings.getFrom());
        message.setTo(otp.getEmail());
        message.setSubject("Health Mate " + purpose + " code");
        message.setText("Your Health Mate " + purpose + " code is: " + otp.getOtpCode()
                + "\n\nThis code expires in 10 minutes. Do not share it with anyone."
                + "\nIf you did not request this code, you can ignore this email.");
        try {
            sender.send(message);
        } catch (MailException | IllegalArgumentException ex) {
            throw new OtpDeliveryException();
        }
    }
}
