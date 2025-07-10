package com.yash.usermanagement.service;

import com.yash.usermanagement.dto.CreateNotificationRequest;
import com.yash.usermanagement.dto.NotificationRequest;
import com.yash.usermanagement.model.User;
import io.micronaut.http.client.HttpClient;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class NotificationClientServiceTest {
    @Inject
    NotificationClientService notificationClientService;
    @Inject
    HttpClient httpClient;

    @MockBean(HttpClient.class)
    HttpClient httpClientMock() { return Mockito.mock(HttpClient.class); }

    @Test
    void testSendUserCreationNotification() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("John");
        Mockito.when(httpClient.exchange(Mockito.any(io.micronaut.http.HttpRequest.class))).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> notificationClientService.sendUserCreationNotification(user, "Bearer token").block());
    }

    @Test
    void testSendAccountDeletionNotification() {
        Mockito.when(httpClient.exchange(Mockito.any(io.micronaut.http.HttpRequest.class))).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> notificationClientService.sendAccountDeletionNotification(UUID.randomUUID(), "test@example.com", "Bearer token").block());
    }

    @Test
    void testSendPasswordResetRequestNotification() {
        Mockito.when(httpClient.exchange(Mockito.any(io.micronaut.http.HttpRequest.class))).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> notificationClientService.sendPasswordResetRequestNotification(UUID.randomUUID(), "test@example.com").block());
    }

    @Test
    void testSendPasswordResetApprovalNotification() {
        Mockito.when(httpClient.exchange(Mockito.any(io.micronaut.http.HttpRequest.class))).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> notificationClientService.sendPasswordResetApprovalNotification(UUID.randomUUID(), "test@example.com").block());
    }

    @Test
    void testSendPasswordChangeRejectionNotification() {
        Mockito.when(httpClient.exchange(Mockito.any(io.micronaut.http.HttpRequest.class))).thenReturn(Mono.empty());
        assertDoesNotThrow(() -> notificationClientService.sendPasswordChangeRejectionNotification(UUID.randomUUID(), "test@example.com").block());
    }
} 