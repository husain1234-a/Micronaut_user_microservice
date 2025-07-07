package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;
import com.yash.usermanagement.dto.FcmRegistrationRequest;
import com.yash.usermanagement.exception.AuthenticationException;
import com.yash.usermanagement.service.AuthenticationService;
import com.yash.usermanagement.service.UserService;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
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
import reactor.core.publisher.Mono;
import java.security.Principal;
import io.micronaut.http.MutableHttpResponse;

@Controller("/api/auth")
@Tag(name = "Authentication")
public class AuthenticationController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthenticationController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    @Post("/login")
    @Operation(summary = "Login user")
    @Secured(SecurityRule.IS_ANONYMOUS)
    public Mono<HttpResponse<LoginResponseDTO>> login(@Body @Valid LoginRequestDTO loginRequest) {
        LOG.info("Login request received for user: {}", loginRequest.getEmail());
        return authenticationService.login(loginRequest)
                .map(HttpResponse::ok);
    }

    @Post("/logout")
    @Operation(summary = "Logout user")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public Mono<HttpResponse<Void>> logout(@Header(HttpHeaders.AUTHORIZATION) String authorization) {
        LOG.info("Logout request received");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.error(new AuthenticationException("Invalid authorization header"));
        }
        String token = authorization.substring(7); // Remove "Bearer " prefix
        return authenticationService.logout(token)
                .thenReturn(HttpResponse.ok());
    }

    @Post("/fcm/register")
    @Operation(summary = "Register FCM token for user")
    @Secured(SecurityRule.IS_AUTHENTICATED)
    public Mono<MutableHttpResponse<Void>> registerFcmToken(@Valid @Body FcmRegistrationRequest request,
            Principal principal) {
        LOG.info("/api/auth/fcm/register called by user: {} with token: {}", principal.getName(), request.getToken());
        return userService.registerFcmToken(request.getToken(), principal.getName())
                .then(Mono.just(HttpResponse.ok((Void) null)))
                .onErrorResume(e -> {
                    LOG.error("Error registering FCM token for user: {}: {}", principal.getName(), e.getMessage(), e);
                    return Mono.just(HttpResponse.serverError());
                });
    }
}