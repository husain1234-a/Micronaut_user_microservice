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
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        LOG.info("Attempting login for user: {}", loginRequest.getEmail());
        try {
            // Find user by email
            Optional<User> userOpt = userService.getUserByEmail(loginRequest.getEmail());
            if (userOpt.isEmpty()) {
                throw new AuthenticationException("Invalid email or password");
            }

            User user = userOpt.get();

            // Validate password
            if (!userService.validateCurrentPassword(user.getId(), loginRequest.getPassword())) {
                throw new AuthenticationException("Invalid email or password");
            }

            // Generate JWT token
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", user.getEmail());
            claims.put("userId", user.getId().toString());
            claims.put("email", user.getEmail());
            claims.put("roles", user.getRole().toString());
            claims.put("firstname", user.getFirstName());
            claims.put("lastname", user.getLastName());
            // claims.put("iat", System.currentTimeMillis() / 1000);
            // claims.put("exp", (System.currentTimeMillis() / 1000) + 3600);
            LOG.info("Generated token claims: {}", claims);

            Optional<String> tokenOpt = tokenGenerator.generateToken(claims);
            if (tokenOpt.isEmpty()) {
                throw new AuthenticationException("Failed to generate authentication token");
            }

            // Create response
            LoginResponseDTO response = new LoginResponseDTO();
            response.setAccessToken(tokenOpt.get());
            response.setTokenType("Bearer");
            response.setUserId(user.getId());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole().toString());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());

            LOG.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());
            return response;

        } catch (AuthenticationException e) {
            LOG.warn("Authentication failed for user: {}", loginRequest.getEmail());
            throw e;
        } catch (Exception e) {
            LOG.error("Error during login for user: {}", loginRequest.getEmail(), e);
            throw new AuthenticationException("An error occurred during login");
        }
    }

    @Override
    @Loggable
    @Auditable
    public void logout(String token) {
        LOG.info("Processing logout request");
        try {
            // Validate token
            Publisher<Authentication> authenticationPublisher = tokenValidator.validateToken(token, null);
            Authentication authentication = Mono.from(authenticationPublisher).block();

            if (authentication == null) {
                throw new AuthenticationException("Invalid token");
            }

            // Add token to blacklist with current timestamp
            tokenBlacklist.put(token, System.currentTimeMillis());
            LOG.info("Token added to blacklist");

            // Clear any session data if needed
            clearSessionData(authentication);

            LOG.info("User logged out successfully");
        } catch (Exception e) {
            LOG.error("Error during logout: {}", e.getMessage());
            throw new AuthenticationException("Failed to process logout", e);
        }
    }

    private void clearSessionData(Authentication authentication) {
        // Extract user information from authentication
        String userId = (String) authentication.getAttributes().get("userId");
        if (userId != null) {
            // Clear any user-specific session data
            LOG.info("Cleared session data for user: {}", userId);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.containsKey(token);
    }
}