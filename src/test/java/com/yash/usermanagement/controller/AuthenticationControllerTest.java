package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;
import com.yash.usermanagement.dto.FcmRegistrationRequest;
import com.yash.usermanagement.service.AuthenticationService;
import com.yash.usermanagement.service.UserService;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class AuthenticationControllerTest {
    @Inject
    AuthenticationController authenticationController;

    @Inject
    AuthenticationService authenticationService;

    @Inject
    UserService userService;

    @MockBean(AuthenticationService.class)
    AuthenticationService authenticationServiceMock() {
        return Mockito.mock(AuthenticationService.class);
    }

    @MockBean(UserService.class)
    UserService userServiceMock() {
        return Mockito.mock(UserService.class);
    }

    @Test
    void testLogin() {
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("ValidPass123!");
        LoginResponseDTO resp = new LoginResponseDTO();
        Mockito.when(authenticationService.login(Mockito.any(LoginRequestDTO.class))).thenReturn(Mono.just(resp));
        Mono<HttpResponse<LoginResponseDTO>> response = authenticationController.login(loginRequest);
        assertEquals(HttpStatus.OK, response.block().getStatus());
    }

    @Test
    void testLogoutWithValidToken() {
        String token = "Bearer valid.jwt.token";
        Mockito.when(authenticationService.logout(Mockito.anyString())).thenReturn(Mono.empty());
        Mono<HttpResponse<Void>> response = authenticationController.logout(token);
        assertEquals(HttpStatus.OK, response.block().getStatus());
    }

    @Test
    void testLogoutWithInvalidToken() {
        String token = "InvalidToken";
        Mono<HttpResponse<Void>> response = authenticationController.logout(token);
        assertThrows(Exception.class, () -> response.block());
    }

    @Test
    void testRegisterFcmToken() {
        FcmRegistrationRequest req = new FcmRegistrationRequest();
        req.setToken("dummy-token");
        Principal principal = Mockito.mock(Principal.class);
        Mockito.when(principal.getName()).thenReturn("user@example.com");
        Mockito.when(userService.registerFcmToken(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mono<io.micronaut.http.MutableHttpResponse<Void>> response = authenticationController.registerFcmToken(req,
                principal);
        assertEquals(HttpStatus.OK, response.block().getStatus());
    }
} 