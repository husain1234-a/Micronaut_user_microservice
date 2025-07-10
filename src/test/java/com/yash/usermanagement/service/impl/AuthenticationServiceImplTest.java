package com.yash.usermanagement.service.impl;

import com.yash.usermanagement.dto.LoginRequestDTO;
import com.yash.usermanagement.dto.LoginResponseDTO;
import com.yash.usermanagement.exception.AuthenticationException;
import com.yash.usermanagement.model.User;
import com.yash.usermanagement.model.UserRole;
import com.yash.usermanagement.service.UserService;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.generator.TokenGenerator;
import io.micronaut.security.token.validator.TokenValidator;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import java.util.Optional;
import java.util.UUID;
import org.reactivestreams.Publisher;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class AuthenticationServiceImplTest {
    @Inject
    AuthenticationServiceImpl authenticationService;
    @Inject
    UserService userService;
    @Inject
    TokenGenerator tokenGenerator;
    @Inject
    TokenValidator tokenValidator;

    @MockBean(UserService.class)
    UserService userServiceMock() { return Mockito.mock(UserService.class); }
    @MockBean(TokenGenerator.class)
    TokenGenerator tokenGeneratorMock() { return Mockito.mock(TokenGenerator.class); }
    @MockBean(TokenValidator.class)
    TokenValidator tokenValidatorMock() { return Mockito.mock(TokenValidator.class); }

    @Test
    void testLoginSuccess() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("user@example.com");
        req.setPassword("password");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setFirstName("John");
        user.setLastName("Doe");
        Mockito.when(userService.getUserByEmail("user@example.com")).thenReturn(Mono.just(user));
        Mockito.when(userService.validateCurrentPassword(user.getId(), "password")).thenReturn(Mono.just(true));
        Mockito.when(tokenGenerator.generateToken(Mockito.anyMap())).thenReturn(Optional.of("token"));
        Mono<LoginResponseDTO> result = authenticationService.login(req);
        assertEquals("token", result.block().getAccessToken());
    }

    @Test
    void testLoginInvalidPassword() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("user@example.com");
        req.setPassword("wrong");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        Mockito.when(userService.getUserByEmail("user@example.com")).thenReturn(Mono.just(user));
        Mockito.when(userService.validateCurrentPassword(user.getId(), "wrong")).thenReturn(Mono.just(false));
        Mono<LoginResponseDTO> result = authenticationService.login(req);
        assertThrows(Exception.class, () -> result.block());
    }

    @Test
    void testLoginUserNotFound() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("notfound@example.com");
        Mockito.when(userService.getUserByEmail("notfound@example.com")).thenReturn(Mono.empty());
        Mono<LoginResponseDTO> result = authenticationService.login(req);
        assertThrows(Exception.class, () -> result.block());
    }

    @Test
    void testLogoutSuccess() {
        String token = "valid.jwt.token";
        Authentication authentication = Mockito.mock(Authentication.class);
        Publisher<Authentication> publisher = Mono.just(authentication);
        Mockito.when(tokenValidator.validateToken(token, null)).thenReturn(publisher);
        Mono<Void> result = authenticationService.logout(token);
        assertDoesNotThrow(() -> result.block());
    }

    @Test
    void testLogoutInvalidToken() {
        String token = "invalid.jwt.token";
        Publisher<Authentication> publisher = Mono.empty();
        Mockito.when(tokenValidator.validateToken(token, null)).thenReturn(publisher);
        Mono<Void> result = authenticationService.logout(token);
        assertThrows(Exception.class, () -> result.block());
    }
} 