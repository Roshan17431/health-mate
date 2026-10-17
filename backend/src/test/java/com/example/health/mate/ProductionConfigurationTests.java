package com.example.health.mate;

import com.example.health.mate.config.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.Base64;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductionConfigurationTests {
    private final MockEnvironment env = new MockEnvironment().withProperty("DB_URL", "jdbc:postgresql://db/test")
            .withProperty("DB_USERNAME", "healthmate_app").withProperty("DB_PASSWORD", "test-only-placeholder")
            .withProperty("CORS_ALLOWED_ORIGINS", "https://care.example.test");
    private JwtProperties jwt(String key) { var p = new JwtProperties(); p.setSecret(key); p.setExpirationMs(3600000); return p; }
    private CorsProperties origins(String origin) { var p = new CorsProperties(); p.setAllowedOrigins(List.of(origin)); return p; }
    private String testKey() { byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes); return Base64.getEncoder().encodeToString(bytes); }
    @Test void safeExplicitConfigurationAccepted() {
        assertDoesNotThrow(() -> new ProductionGuard(jwt(testKey()), origins("https://care.example.test"), env).afterPropertiesSet());
    }
    @Test void knownDevelopmentShortAndMalformedKeysRejectedWithoutDisclosure() {
        for (String value : new String[]{"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", Base64.getEncoder().encodeToString("short".getBytes()), "invalid!", ""}) {
            var error = assertThrows(IllegalStateException.class, () -> new ProductionGuard(jwt(value), origins("https://care.example.test"), env).afterPropertiesSet());
            if (!value.isEmpty()) assertFalse(error.getMessage().contains(value));
        }
    }
    @Test void implicitDatabaseCredentialsAndWildcardOriginsRejected() {
        assertThrows(IllegalStateException.class, () -> new ProductionGuard(jwt(testKey()), origins("https://care.example.test"), new MockEnvironment()).afterPropertiesSet());
        for (String origin : new String[]{"*", "https://*.example.test", "http://care.example.test", "https://care.example.test/path"})
            assertThrows(IllegalStateException.class, () -> new ProductionGuard(jwt(testKey()), origins(origin), env).afterPropertiesSet());
    }
}
