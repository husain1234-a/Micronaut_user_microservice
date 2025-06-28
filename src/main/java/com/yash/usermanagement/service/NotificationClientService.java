package com.yash.usermanagement.service;

import com.yash.usermanagement.dto.NotificationRequest;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import jakarta.inject.Singleton;

@Singleton
public class NotificationClientService {
    private final HttpClient httpClient;
    private final String notificationServiceUrl;

    public NotificationClientService(@Client("${notification.service.url}") HttpClient httpClient,
                                     @Value("${notification.service.url}") String notificationServiceUrl) {
        this.httpClient = httpClient;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    public void sendUserCreationNotification(String recipient, String message) {
        NotificationRequest request = new NotificationRequest(recipient, message);
        HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/user-creation", request);
        httpClient.toBlocking().exchange(httpRequest);
    }

    public void sendAccountDeletionNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your account was deleted");
        HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/account-deletion", request);
        httpClient.toBlocking().exchange(httpRequest);
    }

    public void sendPasswordResetRequestNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your password reset request is pending approval");
        HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/password-reset-request", request);
        httpClient.toBlocking().exchange(httpRequest);
    }

    public void sendPasswordResetApprovalNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your password reset request has been approved.");
        HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/password-reset-approval", request);
        httpClient.toBlocking().exchange(httpRequest);
    }

    public void sendPasswordChangeRejectionNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your password reset request has been rejected.");
        HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/password-reset-rejection", request);
        httpClient.toBlocking().exchange(httpRequest);
    }
}