package com.example.attendance.exception;

public class OrganizerCreationException extends RuntimeException {
    public OrganizerCreationException(String message) {
        super(message);
    }

    public OrganizerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}