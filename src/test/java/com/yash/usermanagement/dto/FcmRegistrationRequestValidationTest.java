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
class FcmRegistrationRequestValidationTest {
    private final Validator validator;

    public FcmRegistrationRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidFcmRegistrationRequest() {
        FcmRegistrationRequest req = new FcmRegistrationRequest();
        req.setToken("some-token");
        Set<ConstraintViolation<FcmRegistrationRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankFcmToken() {
        FcmRegistrationRequest req = new FcmRegistrationRequest();
        req.setToken("");
        Set<ConstraintViolation<FcmRegistrationRequest>> violations = validator.validate(req);
        assertFalse(violations.isEmpty()); // Violation aani chahiye
    }
}