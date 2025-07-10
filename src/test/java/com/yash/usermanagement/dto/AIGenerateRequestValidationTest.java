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
class AIGenerateRequestValidationTest {
    private final Validator validator;

    public AIGenerateRequestValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void testValidAIGenerateRequest() {
        AIGenerateRequest req = new AIGenerateRequest();
        req.setPrompt("Generate a summary for this text.");
        Set<ConstraintViolation<AIGenerateRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBlankPrompt() {
        AIGenerateRequest req = new AIGenerateRequest();
        req.setPrompt("");
        Set<ConstraintViolation<AIGenerateRequest>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("prompt")));
    }
} 