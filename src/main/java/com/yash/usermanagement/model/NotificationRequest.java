package com.yash.usermanagement.model;

public class NotificationRequest {
    private String recipient;
    private String message;

    public NotificationRequest() {
    }

    public NotificationRequest(String recipient, String message) {
        this.recipient = recipient;
        this.message = message;
    }

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
