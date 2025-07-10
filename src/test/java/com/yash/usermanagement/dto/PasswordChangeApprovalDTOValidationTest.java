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
class PasswordChangeApprovalDTOValidationTest {
    private final Validator validator;

    public PasswordChangeApprovalDTOValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidPasswordChangeApprovalDTO() {
        PasswordChangeApprovalDTO dto = new PasswordChangeApprovalDTO();
        dto.setAdminId(java.util.UUID.randomUUID());
        dto.setApproved(true);
        Set<ConstraintViolation<PasswordChangeApprovalDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testNullRequestId() {
        PasswordChangeApprovalDTO dto = new PasswordChangeApprovalDTO();
        dto.setAdminId(null);
        dto.setApproved(false);
        Set<ConstraintViolation<PasswordChangeApprovalDTO>> violations = validator.validate(dto);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("adminId")));
    }

    @Test
    void testNullUserId() {
        PasswordChangeApprovalDTO dto = new PasswordChangeApprovalDTO();
        dto.setAdminId(null);
        dto.setApproved(true);
        Set<ConstraintViolation<PasswordChangeApprovalDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty()); // Violation aani chahiye
    }
}