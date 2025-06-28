package com.yash.usermanagement.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum UserRole {
    ADMIN,
    USER
}