package com.yash.usermanagement.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class NotificationRequest {
    private String recipient;
    private String message;

    // Constructors
    public NotificationRequest() {
    }

    public NotificationRequest(String recipient, String message) {
        this.recipient = recipient;
        this.message = message;
    }

    // Getters and setters
    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
