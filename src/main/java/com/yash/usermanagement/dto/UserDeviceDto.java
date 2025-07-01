package com.yash.usermanagement.dto;

import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;
import java.util.UUID;

@Serdeable
public class UserDeviceDto {
    private UUID id;

    private UUID userId;

    private String fcmToken;

    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UserDeviceDto(UUID id, UUID userId, String fcmToken, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.createdAt = createdAt;
    }

}
