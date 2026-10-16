package com.example.health.mate.exception;

public class OtpRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public OtpRateLimitException(long retryAfterSeconds) {
        super("Too many verification requests. Please try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
