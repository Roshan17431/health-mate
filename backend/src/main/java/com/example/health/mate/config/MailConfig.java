package com.example.health.mate.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailSettings.class)
public class MailConfig {
    @Bean
    JavaMailSender mailSender(MailSettings settings) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.getHost());
        sender.setPort(settings.getPort());
        sender.setUsername(settings.getUsername());
        sender.setPassword(settings.getPassword());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", Boolean.toString(settings.isAuth()));
        properties.setProperty("mail.smtp.starttls.enable", Boolean.toString(settings.isStarttlsEnabled()));
        properties.setProperty("mail.smtp.starttls.required", Boolean.toString(settings.isStarttlsRequired()));
        properties.setProperty("mail.smtp.ssl.enable", Boolean.toString(settings.isSslEnabled()));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", Integer.toString(settings.getConnectionTimeoutMs()));
        properties.setProperty("mail.smtp.timeout", Integer.toString(settings.getTimeoutMs()));
        properties.setProperty("mail.smtp.writetimeout", Integer.toString(settings.getWriteTimeoutMs()));
        return sender;
    }
}
