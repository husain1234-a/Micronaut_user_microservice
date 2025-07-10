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
class CreateUserRequestValidationTest {
    private final Validator validator;

    public CreateUserRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidCreateUserRequest() {
        // TODO: Create a valid CreateUserRequest instance and assert no violations
    }

    @Test
    void testInvalidEmail() {
        // TODO: Create a CreateUserRequest with invalid email and assert violations
    }

    @Test
    void testBlankFirstName() {
        // TODO: Create a CreateUserRequest with blank first name and assert violations
    }

    // Add more tests for other validation constraints
} 