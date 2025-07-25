// package com.yash.usermanagement.service;

// import com.yash.usermanagement.dto.NotificationRequest;
// import io.micronaut.context.annotation.Value;
// import io.micronaut.http.HttpRequest;
// import io.micronaut.http.client.HttpClient;
// import io.micronaut.http.client.annotation.Client;
// import jakarta.inject.Singleton;

// @Singleton
// public class NotificationClientService {
//     private final HttpClient httpClient;
//     private final String notificationServiceUrl;

//     public NotificationClientService(@Client("${notification.service.url}") HttpClient httpClient,
//                                      @Value("${notification.service.url}") String notificationServiceUrl) {
//         this.httpClient = httpClient;
//         this.notificationServiceUrl = notificationServiceUrl;
//     }

//     public void sendUserCreationNotification(String recipient, String message) {
//         NotificationRequest request = new NotificationRequest(recipient, message);
//         HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/user-creation", request);
//         httpClient.toBlocking().exchange(httpRequest);
//     }

//     public void sendAccountDeletionNotification(Object userId, String email) {
//         NotificationRequest request = new NotificationRequest(email, "Your account was deleted");
//         HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/account-deletion", request);
//         httpClient.toBlocking().exchange(httpRequest);
//     }

//     public void sendPasswordResetRequestNotification(Object userId, String email) {
//         NotificationRequest request = new NotificationRequest(email, "Your password reset request is pending approval");
//         HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/password-reset-request", request);
//         httpClient.toBlocking().exchange(httpRequest);
//     }

//     public void sendPasswordResetApprovalNotification(Object userId, String email) {
//         NotificationRequest request = new NotificationRequest(email, "Your password reset request has been approved.");
//         HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/password-reset-approval", request);
//         httpClient.toBlocking().exchange(httpRequest);
//     }

//     public void sendPasswordChangeRejectionNotification(Object userId, String email) {
//         NotificationRequest request = new NotificationRequest(email, "Your password reset request has been rejected.");
//         HttpRequest<NotificationRequest> httpRequest = HttpRequest.POST(notificationServiceUrl + "/notify/password-reset-rejection", request);
//         httpClient.toBlocking().exchange(httpRequest);
//     }
// }


package com.yash.usermanagement.service;

import com.yash.usermanagement.dto.CreateNotificationRequest;
import com.yash.usermanagement.dto.NotificationRequest;
import com.yash.usermanagement.model.User;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Singleton;

import java.util.Optional;
import reactor.core.publisher.Mono;

@Singleton
public class NotificationClientService {
    private final HttpClient httpClient;
    private final String notificationServiceUrl;

    public NotificationClientService(@Client("${notification.service.url}") HttpClient httpClient,
                                     @Value("${notification.service.url}") String notificationServiceUrl) {
        this.httpClient = httpClient;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    private Optional<String> getAuthorizationHeader() {
        return ServerRequestContext.currentRequest()
                .map(req -> req.getHeaders().get(HttpHeaders.AUTHORIZATION));
    }

    private Mono<Void> sendNotification(String path, Object request, String authorization) {
        MutableHttpRequest<Object> httpRequest = HttpRequest.POST(notificationServiceUrl + path, request);
        if (authorization != null) {
            httpRequest.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        return Mono.from(httpClient.exchange(httpRequest)).then();
    }

    public Mono<Void> sendUserCreationNotification(User user, String authorization) {
        CreateNotificationRequest request = new CreateNotificationRequest(
            user.getId(),
            "Welcome " + user.getFirstName() + "!",
            "Your account has been created successfully."
        );
        return sendNotification("/api/notifications/user-creation", request, authorization);
    }

    public Mono<Void> sendAccountDeletionNotification(Object userId, String email, String authorization) {
        NotificationRequest request = new NotificationRequest(email, "Your account was deleted");
        return sendNotification("/notify/account-deletion", request, authorization);
    }

    public Mono<Void> sendPasswordResetRequestNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your password reset request is pending approval");
        return sendNotification("/notify/password-reset-request", request, null); // No specific authorization needed for this one
    }

    public Mono<Void> sendPasswordResetApprovalNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your password reset request has been approved.");
        return sendNotification("/notify/password-reset-approval", request, null); // No specific authorization needed for this one
    }

    public Mono<Void> sendPasswordChangeRejectionNotification(Object userId, String email) {
        NotificationRequest request = new NotificationRequest(email, "Your password reset request has been rejected.");
        return sendNotification("/notify/password-reset-rejection", request, null); // No specific authorization needed for this one
    }
}