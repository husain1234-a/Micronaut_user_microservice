package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.*;
import com.yash.usermanagement.model.User;
import com.yash.usermanagement.model.Address;
import com.yash.usermanagement.model.UserDevice;
import com.yash.usermanagement.service.UserService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import io.micronaut.test.annotation.MockBean;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@MicronautTest
class UserControllerTest {
    @Inject
    UserController userController;

    @Inject
    UserService userService;

    @MockBean(UserService.class)
    UserService userServiceMock() {
        return Mockito.mock(UserService.class);
    }

    @Test
    void testCreateUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setEmail("john.doe@example.com");
        req.setPassword("Test@123456");
        req.setRole(com.yash.usermanagement.model.UserRole.USER);
        req.setDateOfBirth(java.time.LocalDate.of(1990, 1, 1));
        req.setGender(com.yash.usermanagement.model.Gender.MALE);
        req.setPhoneNumber("9876543210");
        User user = new User();
        user.setId(UUID.randomUUID());
        Mockito.when(userService.createUser(Mockito.any(User.class))).thenReturn(Mono.just(user));
        Mockito.when(userService.sendUserCreationNotification(Mockito.any(User.class), Mockito.anyString()))
                .thenReturn(Mono.just("Notification sent"));
        Mono<HttpResponse<UserCreationResponse>> response = userController.createUser(req, "Bearer token");
        assertEquals(HttpStatus.CREATED, response.block().getStatus());
    }

    @Test
    void testGetUserById() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        Mockito.when(userService.getUserById(id)).thenReturn(Mono.just(user));
        Mono<HttpResponse<UserResponse>> response = userController.getUserById(id);
        assertEquals(HttpStatus.OK, response.block().getStatus());
    }

    @Test
    void testUpdateUser() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFirstName("Jane");
        req.setLastName("Smith");
        req.setEmail("jane.smith@example.com");
        req.setDateOfBirth(java.time.LocalDate.of(1995, 5, 15));
        req.setGender(com.yash.usermanagement.model.Gender.FEMALE);
        req.setRole(com.yash.usermanagement.model.UserRole.USER);
        req.setPhoneNumber("9876543210");
        User user = new User();
        user.setId(id);
        Mockito.when(userService.updateUser(Mockito.eq(id), Mockito.any(User.class))).thenReturn(Mono.just(user));
        Mono<HttpResponse<UserResponse>> response = userController.updateUser(id, req);
        assertEquals(HttpStatus.OK, response.block().getStatus());
    }

    @Test
    void testDeleteUser() {
        UUID id = UUID.randomUUID();
        Mockito.when(userService.deleteUser(Mockito.eq(id), Mockito.anyString())).thenReturn(Mono.empty());
        Mono<io.micronaut.http.MutableHttpResponse<Map<String, Object>>> response = userController.deleteUser(id,
                "Bearer token");
        assertEquals(HttpStatus.OK, response.block().getStatus());
        assertTrue((Boolean) response.block().body().get("success"));
    }

    @Test
    void testCreateAddress() {
        UUID userId = UUID.randomUUID();
        CreateAddressRequest req = new CreateAddressRequest();
        req.setStreetAddress("123 Main St");
        req.setCity("Metropolis");
        req.setState("State");
        req.setCountry("IN");
        req.setPostalCode("123456");
        req.setAddressType(com.yash.usermanagement.model.AddressType.HOME);
        Address address = new Address();
        address.setId(UUID.randomUUID());
        Mockito.when(userService.createAddress(Mockito.any(Address.class))).thenReturn(Mono.just(address));
        Mono<HttpResponse<AddressResponse>> response = userController.createAddress(userId, req);
        assertEquals(HttpStatus.CREATED, response.block().getStatus());
    }

    @Test
    void testRequestPasswordChange() {
        UUID id = UUID.randomUUID();
        PasswordChangeRequestDTO req = new PasswordChangeRequestDTO();
        req.setOldPassword("OldPass123!");
        req.setNewPassword("NewPass123!");
        Mockito.when(userService.requestPasswordChange(Mockito.eq(id), Mockito.any(PasswordChangeRequestDTO.class)))
                .thenReturn(Mono.empty());
        Mono<HttpResponse<Void>> response = userController.requestPasswordChange(id, req);
        assertEquals(HttpStatus.ACCEPTED, response.block().getStatus());
    }
}