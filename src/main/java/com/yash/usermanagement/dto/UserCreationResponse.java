package com.yash.usermanagement.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class UserCreationResponse {
    private UserResponse user;
    private String message;

    public UserCreationResponse(UserResponse user, String message) {
        this.user = user;
        this.message = message;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 