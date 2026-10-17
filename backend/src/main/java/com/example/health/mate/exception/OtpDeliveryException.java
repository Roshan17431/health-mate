package com.example.health.mate.exception;

/** Deliberately contains no provider, recipient or verification-code details. */
public class OtpDeliveryException extends RuntimeException {
    public OtpDeliveryException() {
        super("Unable to send verification email. Please try again later.");
    }
}
