package com.yash.usermanagement.model;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum AddressType {
    HOME,
    WORK,
    BILLING,
    SHIPPING
}