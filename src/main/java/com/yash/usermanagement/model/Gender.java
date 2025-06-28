package com.yash.usermanagement.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY
}