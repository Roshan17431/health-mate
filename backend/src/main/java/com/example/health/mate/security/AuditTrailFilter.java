package com.example.health.mate.security;

import com.example.health.mate.entity.User;
import com.example.health.mate.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

/** Records completed mutations without copying passwords, OTPs, tokens or request bodies. */
public class AuditTrailFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditLogService audit;
    public AuditTrailFilter(AuditLogService audit) { this.audit = audit; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
        if (MUTATIONS.contains(request.getMethod()) && request.getRequestURI().startsWith("/api/")
                && !request.getRequestURI().startsWith("/api/auth/")
                && response.getStatus() >= 200 && response.getStatus() < 300) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            Long actor = authentication != null && authentication.getPrincipal() instanceof User user ? user.getId() : null;
            audit.logAction(actor, request.getMethod() + " " + request.getRequestURI(), "Completed with status " + response.getStatus());
        }
    }
}
