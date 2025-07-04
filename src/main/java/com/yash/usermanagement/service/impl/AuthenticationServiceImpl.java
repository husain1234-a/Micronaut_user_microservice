package com.yash.usermanagement.service.impl;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;
import com.yash.usermanagement.exception.AuthenticationException;
import com.yash.usermanagement.model.User;
import com.yash.usermanagement.service.AuthenticationService;
import com.yash.usermanagement.service.UserService;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import io.micronaut.security.token.validator.TokenValidator;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import com.yash.usermanagement.aop.Loggable;
import com.yash.usermanagement.aop.Auditable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private final UserService userService;
    private final TokenGenerator tokenGenerator;
    private final TokenValidator tokenValidator;
    private final Map<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    public AuthenticationServiceImpl(UserService userService, TokenGenerator tokenGenerator,
            TokenValidator tokenValidator) {
        this.userService = userService;
        this.tokenGenerator = tokenGenerator;
        this.tokenValidator = tokenValidator;
    }

    @Override
    @Loggable
    @Auditable
    public Mono<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        LOG.info("Attempting login for user: {}", loginRequest.getEmail());
        return userService.getUserByEmail(loginRequest.getEmail())
            .switchIfEmpty(Mono.error(new AuthenticationException("Invalid email or password")))
            .flatMap(user -> userService.validateCurrentPassword(user.getId(), loginRequest.getPassword())
                .flatMap(isValid -> {
                    if (!isValid) {
                        return Mono.error(new AuthenticationException("Invalid email or password"));
                    }
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("sub", user.getEmail());
                    claims.put("userId", user.getId().toString());
                    claims.put("email", user.getEmail());
                    claims.put("roles", user.getRole().toString());
                    claims.put("firstname", user.getFirstName());
                    claims.put("lastname", user.getLastName());
                    LOG.info("Generated token claims: {}", claims);
                    Optional<String> tokenOpt = tokenGenerator.generateToken(claims);
                    if (tokenOpt.isEmpty()) {
                        return Mono.error(new AuthenticationException("Failed to generate authentication token"));
                    }
                    LoginResponseDTO response = new LoginResponseDTO();
                    response.setAccessToken(tokenOpt.get());
                    response.setTokenType("Bearer");
                    response.setUserId(user.getId());
                    response.setEmail(user.getEmail());
                    response.setRole(user.getRole().toString());
                    response.setFirstName(user.getFirstName());
                    response.setLastName(user.getLastName());
                    LOG.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());
                    return Mono.just(response);
                })
            );
    }

    @Override
    @Loggable
    @Auditable
    public Mono<Void> logout(String token) {
        LOG.info("Processing logout request");
        return Mono.from(tokenValidator.validateToken(token, null))
            .cast(Authentication.class)
            .switchIfEmpty(Mono.error(new AuthenticationException("Invalid token")))
            .flatMap(authentication -> {
                Authentication auth = (Authentication) authentication;
                tokenBlacklist.put(token, System.currentTimeMillis());
                clearSessionData(auth);
                LOG.info("User logged out successfully");
                return Mono.<Void>empty();
            });
    }

    private void clearSessionData(Authentication authentication) {
        String userId = (String) authentication.getAttributes().get("userId");
        if (userId != null) {
            LOG.info("Cleared session data for user: {}", userId);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.containsKey(token);
    }
}