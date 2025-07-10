package com.yash.usermanagement.service.impl;

import com.yash.usermanagement.model.User;
import com.yash.usermanagement.model.UserRole;
import com.yash.usermanagement.repository.UserRepository;
import com.yash.usermanagement.repository.AddressRepository;
import com.yash.usermanagement.repository.PasswordChangeRequestRepository;
import com.yash.usermanagement.repository.UserDeviceRepository;
import com.yash.usermanagement.service.NotificationClientService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class UserServiceImplTest {
    @Inject
    UserServiceImpl userService;
    @Inject
    UserRepository userRepository;
    @Inject
    AddressRepository addressRepository;
    @Inject
    PasswordChangeRequestRepository passwordChangeRequestRepository;
    @Inject
    UserDeviceRepository userDeviceRepository;
    @Inject
    NotificationClientService notificationClientService;

    @MockBean(UserRepository.class)
    UserRepository userRepositoryMock() { return Mockito.mock(UserRepository.class); }
    @MockBean(AddressRepository.class)
    AddressRepository addressRepositoryMock() { return Mockito.mock(AddressRepository.class); }
    @MockBean(PasswordChangeRequestRepository.class)
    PasswordChangeRequestRepository passwordChangeRequestRepositoryMock() { return Mockito.mock(PasswordChangeRequestRepository.class); }
    @MockBean(UserDeviceRepository.class)
    UserDeviceRepository userDeviceRepositoryMock() { return Mockito.mock(UserDeviceRepository.class); }
    @MockBean(NotificationClientService.class)
    NotificationClientService notificationClientServiceMock() { return Mockito.mock(NotificationClientService.class); }

    @Test
    void testCreateUserWhenEmailNotExists() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(UserRole.USER);
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());
        Mockito.when(userRepository.save(user)).thenReturn(Mono.just(user));
        Mono<User> result = userService.createUser(user);
        assertEquals("test@example.com", result.block().getEmail());
    }

    @Test
    void testCreateUserWhenEmailExists() {
        User user = new User();
        user.setEmail("test@example.com");
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(user));
        Mono<User> result = userService.createUser(user);
        assertThrows(Exception.class, () -> result.block());
    }

    @Test
    void testGetUserByIdFound() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(user));
        Mono<User> result = userService.getUserById(id);
        assertEquals(id, result.block().getId());
    }

    @Test
    void testGetUserByIdNotFound() {
        UUID id = UUID.randomUUID();
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.empty());
        Mono<User> result = userService.getUserById(id);
        assertThrows(Exception.class, () -> result.block());
    }

    @Test
    void testUpdateUser() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);
        existing.setFirstName("Old");
        User update = new User();
        update.setFirstName("New");
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(existing));
        Mockito.when(userRepository.update(Mockito.any(User.class))).thenReturn(Mono.just(existing));
        Mono<User> result = userService.updateUser(id, update);
        assertEquals("New", result.block().getFirstName());
    }

    @Test
    void testDeleteUser() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(user));
        Mockito.when(userRepository.deleteById(id)).thenReturn(Mono.empty());
        Mono<Void> result = userService.deleteUser(id, "token");
        assertDoesNotThrow(() -> result.block());
    }
} 