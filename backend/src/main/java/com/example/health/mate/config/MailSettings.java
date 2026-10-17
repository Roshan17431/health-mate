package com.example.health.mate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailSettings {
    private String host = "";
    private int port = 587;
    private String username = "";
    private String password = "";
    private String from = "";
    private boolean auth = true;
    private boolean starttlsEnabled = true;
    private boolean starttlsRequired = true;
    private boolean sslEnabled;
    private int connectionTimeoutMs = 5000;
    private int timeoutMs = 5000;
    private int writeTimeoutMs = 5000;
}
