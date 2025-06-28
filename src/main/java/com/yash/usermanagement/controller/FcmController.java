package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.FcmRegistrationRequest;
import com.yash.usermanagement.service.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;

@Controller("/api/fcm")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class FcmController {

    private static final Logger LOG = LoggerFactory.getLogger(FcmController.class);

    private final UserService userService;

    public FcmController(UserService userService) {
        this.userService = userService;
    }

    @Post("/register")
    public HttpResponse<Void> registerFcmToken(@Valid @Body FcmRegistrationRequest request, Principal principal) {
        LOG.info("/api/fcm/register called by user: {} with token: {}", principal.getName(), request.getToken());
        try {
            userService.registerFcmToken(request.getToken(), principal.getName());
            LOG.info("Successfully registered FCM token for user: {}", principal.getName());
            return HttpResponse.ok();
        } catch (Exception e) {
            LOG.error("Error registering FCM token for user: {}: {}", principal.getName(), e.getMessage(), e);
            return HttpResponse.serverError();
        }
    }
} 