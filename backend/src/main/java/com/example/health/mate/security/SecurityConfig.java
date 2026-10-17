package com.example.health.mate.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.example.health.mate.service.AuditLogService audit;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, com.example.health.mate.service.AuditLogService audit) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.audit = audit;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401); response.setContentType("application/json");
                            response.getWriter().write("{\"statusCode\":401,\"message\":\"Please sign in again.\"}");
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403); response.setContentType("application/json");
                            response.getWriter().write("{\"statusCode\":403,\"message\":\"You do not have permission for this action.\"}");
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/otp/send",
                                "/api/otp/verify",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/audit-logs/**").denyAll()
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/audit-logs/**").denyAll()
                        .requestMatchers("/actuator/**", "/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new AuditTrailFilter(audit), JwtAuthenticationFilter.class)
                .build();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
