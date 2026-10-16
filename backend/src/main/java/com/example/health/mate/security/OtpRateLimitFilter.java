package com.example.health.mate.security;

import com.example.health.mate.exception.OtpRateLimitException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OtpRateLimitFilter extends OncePerRequestFilter {
    private final OtpRequestLimiter limiter;
    public OtpRateLimitFilter(OtpRequestLimiter limiter) { this.limiter = limiter; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("POST".equals(request.getMethod()) && ("/api/otp/send".equals(path) || "/api/otp/verify".equals(path))) {
            try {
                limiter.checkIp(request.getRemoteAddr(), path.endsWith("/send"));
            } catch (OtpRateLimitException ex) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(ex.getRetryAfterSeconds()));
                response.setContentType("application/json");
                response.getWriter().write("{\"statusCode\":429,\"message\":\"Too many verification requests. Please try again later.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
