package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;
import com.yash.usermanagement.exception.AuthenticationException;
import com.yash.usermanagement.service.AuthenticationService;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/auth")
@Tag(name = "Authentication")
public class AuthenticationController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Post("/login")
    @Operation(summary = "Login user")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public HttpResponse<LoginResponseDTO> login(@Body @Valid LoginRequestDTO loginRequest) {
        LOG.info("Login request received for user: {}", loginRequest.getEmail());
        try {
            LoginResponseDTO response = authenticationService.login(loginRequest);
            return HttpResponse.ok(response);
        } catch (AuthenticationException e) {
            LOG.warn("Login failed for user: {}", loginRequest.getEmail());
            throw e;
        }
    }

    @Post("/logout")
    @Operation(summary = "Logout user")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public HttpResponse<Void> logout(@Header(HttpHeaders.AUTHORIZATION) String authorization) {
        LOG.info("Logout request received");
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new AuthenticationException("Invalid authorization header");
            }

            String token = authorization.substring(7); // Remove "Bearer " prefix
            authenticationService.logout(token);
            return HttpResponse.ok();
        } catch (AuthenticationException e) {
            LOG.warn("Logout failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error during logout", e);
            throw new AuthenticationException("An error occurred during logout");
        }
    }
}