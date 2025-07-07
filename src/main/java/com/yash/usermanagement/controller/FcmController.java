package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.FcmRegistrationRequest;
import com.yash.usermanagement.service.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import reactor.core.publisher.Mono;

@Controller("/api/fcm")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class FcmController {

    private static final Logger LOG = LoggerFactory.getLogger(FcmController.class);

    private final UserService userService;

    public FcmController(UserService userService) {
        this.userService = userService;
    }

    @Post("/register")
    public Mono<? extends MutableHttpResponse<?>> registerFcmToken(@Valid @Body FcmRegistrationRequest request, Principal principal) {
        LOG.info("/api/fcm/register called by user: {} with token: {}", principal.getName(), request.getToken());
        return userService.registerFcmToken(request.getToken(), principal.getName())
            .doOnSubscribe(sub -> LOG.info("Starting FCM token registration for user: {}", principal.getName()))
            .doOnSuccess(v -> LOG.info("Successfully registered FCM token for user: {}", principal.getName()))
            .thenReturn(HttpResponse.ok())
            .onErrorResume(e -> {
                LOG.error("Error registering FCM token for user: {}: {}", principal.getName(), e.getMessage(), e);
                return Mono.just(HttpResponse.serverError());
            });
    }
} 