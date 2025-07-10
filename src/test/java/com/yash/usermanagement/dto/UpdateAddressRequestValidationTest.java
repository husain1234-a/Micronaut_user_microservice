package com.yash.usermanagement.dto;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class UpdateAddressRequestValidationTest {
    private final Validator validator;

    public UpdateAddressRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidUpdateAddressRequest() {
        UpdateAddressRequest req = new UpdateAddressRequest();
        req.setStreetAddress("123 Main St");
        req.setCity("Metropolis");
        req.setState("State");
        req.setCountry("IN");
        req.setPostalCode("123456");
        Set<ConstraintViolation<UpdateAddressRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidPostalCode() {
        UpdateAddressRequest req = new UpdateAddressRequest();
        req.setPostalCode("abc");
        Set<ConstraintViolation<UpdateAddressRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("postalCode")));
    }

    @Test
    void testLongStreet() {
        UpdateAddressRequest req = new UpdateAddressRequest();
        req.setStreetAddress("a".repeat(101));
        Set<ConstraintViolation<UpdateAddressRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty()); // Violation aani chahiye
    }
}