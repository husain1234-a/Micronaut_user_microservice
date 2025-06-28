package com.yash.usermanagement.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Serdeable
public class PasswordChangeApprovalDTO {

    @NotNull
    private UUID adminId;

    @NotNull
    private boolean approved;

    public UUID getAdminId() {
        return adminId;
    }

    public void setAdminId(UUID adminId) {
        this.adminId = adminId;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}