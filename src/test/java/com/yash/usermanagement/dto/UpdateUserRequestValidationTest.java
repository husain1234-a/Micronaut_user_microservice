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
class UpdateUserRequestValidationTest {
    private final Validator validator;

    public UpdateUserRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidUpdateUserRequest() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john.doe@example.com");
        req.setPhoneNumber("1234567890");
        req.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testInvalidEmail() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("not-an-email");
        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void testBlankFirstName() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("");
        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("firstName")));
    }

    @Test
    void testNullDateOfBirth() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setDateOfBirth(null);
        Set<ConstraintViolation<UpdateUserRequest>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("dateOfBirth")));
    }
} 