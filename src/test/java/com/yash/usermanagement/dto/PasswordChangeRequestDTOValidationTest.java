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
class PasswordChangeRequestDTOValidationTest {
    private final Validator validator;

    public PasswordChangeRequestDTOValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidPasswordChangeRequestDTO() {
        PasswordChangeRequestDTO req = new PasswordChangeRequestDTO();
        req.setOldPassword("ValidPass123!");
        req.setNewPassword("NewValidPass123!");
        Set<ConstraintViolation<PasswordChangeRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankCurrentPassword() {
        PasswordChangeRequestDTO req = new PasswordChangeRequestDTO();
        req.setOldPassword("");
        req.setNewPassword("NewValidPass123!");
        Set<ConstraintViolation<PasswordChangeRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("oldPassword")));
    }

    @Test
    void testBlankNewPassword() {
        PasswordChangeRequestDTO req = new PasswordChangeRequestDTO();
        req.setOldPassword("ValidPass123!");
        req.setNewPassword("");
        Set<ConstraintViolation<PasswordChangeRequestDTO>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newPassword")));
    }
}