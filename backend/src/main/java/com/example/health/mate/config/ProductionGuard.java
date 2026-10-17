package com.example.health.mate.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

/** Fail closed before serving production traffic; error messages never include secret values. */
@Component
@Profile("prod")
public class ProductionGuard implements InitializingBean {
    private static final String DEVELOPMENT_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private final JwtProperties jwt;
    private final CorsProperties cors;
    private final Environment environment;
    public ProductionGuard(JwtProperties jwt, CorsProperties cors, Environment environment) {
        this.jwt = jwt; this.cors = cors; this.environment = environment;
    }
    @Override public void afterPropertiesSet() {
        byte[] key;
        try { key = Base64.getDecoder().decode(jwt.getSecret()); }
        catch (RuntimeException ex) { throw new IllegalStateException("Production JWT key must be valid Base64."); }
        if (key.length < 32 || MessageDigest.isEqual(key, Base64.getDecoder().decode(DEVELOPMENT_KEY)))
            throw new IllegalStateException("Production JWT key must contain at least 32 private random bytes; development keys are forbidden.");
        for (String name : Set.of("DB_URL", "DB_USERNAME", "DB_PASSWORD", "CORS_ALLOWED_ORIGINS")) {
            String value = environment.getProperty(name);
            if (value == null || value.isBlank() || (value.startsWith("<") && value.endsWith(">")))
                throw new IllegalStateException("Production configuration requires an explicit " + name + ".");
        }
        if (cors.getAllowedOrigins().isEmpty()) throw new IllegalStateException("Production requires an exact CORS origin allowlist.");
        for (String origin : cors.getAllowedOrigins()) {
            try {
                URI uri = URI.create(origin);
                boolean local = Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost());
                if (origin.contains("*") || uri.getHost() == null || uri.getUserInfo() != null
                        || uri.getQuery() != null || uri.getFragment() != null || !uri.getRawPath().isEmpty()
                        || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme()))))
                    throw new IllegalArgumentException();
            } catch (RuntimeException ex) { throw new IllegalStateException("Production CORS entries must be exact HTTPS origins (HTTP is allowed only for loopback staging)."); }
        }
    }
}
