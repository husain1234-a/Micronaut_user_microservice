package com.yash.usermanagement.model;

import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@MappedEntity("password_change_requests")
@Serdeable
public class PasswordChangeRequest {

    @Id
    @AutoPopulated
    private UUID id;

    @NotNull
    @MappedProperty("user_id")
    private UUID userId;

    @NotBlank
    @MappedProperty("new_password")
    private String newPassword;

    @NotNull
    @MappedProperty("status")
    private PasswordChangeStatus status;

    @MappedProperty("admin_id")
    private UUID adminId;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    @MappedProperty("updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
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

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public PasswordChangeStatus getStatus() {
        return status;
    }

    public void setStatus(PasswordChangeStatus status) {
        this.status = status;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}