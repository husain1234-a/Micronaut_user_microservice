package com.yash.usermanagement.service.impl;

import com.yash.usermanagement.model.*;
import com.yash.usermanagement.repository.*;
import com.yash.usermanagement.service.NotificationClientService;
import com.yash.usermanagement.dto.PasswordChangeApprovalDTO;
import com.yash.usermanagement.dto.PasswordChangeRequestDTO;
import com.yash.usermanagement.exception.DatabaseException;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.exception.ValidationException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.*;
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
    void testCreateUserSuccess() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.empty());
        Mockito.when(userRepository.save(user)).thenReturn(Mono.just(user));
        Mono<User> result = userService.createUser(user);
        assertEquals(user, result.block());
    }

    @Test
    void testCreateUserDuplicate() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(user));
        Mono<User> result = userService.createUser(user);
        assertThrows(DatabaseException.class, () -> result.block());
    }

    @Test
    void testGetUserByIdSuccess() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(user));
        Mono<User> result = userService.getUserById(id);
        assertEquals(user, result.block());
    }

    @Test
    void testGetUserByIdNotFound() {
        UUID id = UUID.randomUUID();
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.empty());
        Mono<User> result = userService.getUserById(id);
        assertThrows(ResourceNotFoundException.class, () -> result.block());
    }

    @Test
    void testUpdateUserSuccess() {
        UUID id = UUID.randomUUID();
        User existing = new User();
        existing.setId(id);
        existing.setEmail("old@example.com");
        User updated = new User();
        updated.setId(id);
        updated.setEmail("new@example.com");
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(existing));
        Mockito.when(userRepository.update(Mockito.any(User.class))).thenReturn(Mono.just(updated));
        Mono<User> result = userService.updateUser(id, updated);
        assertEquals("new@example.com", result.block().getEmail());
    }

    @Test
    void testDeleteUserSuccess() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(user));
        Mockito.when(userRepository.deleteById(id)).thenReturn(Mono.empty());
        Mono<Void> result = userService.deleteUser(id, "auth");
        assertDoesNotThrow(() -> { result.block(); });
    }

    @Test
    void testFindByEmailSuccess() {
        User user = new User();
        user.setEmail("test@example.com");
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(user));
        Mono<User> result = userService.findByEmail("test@example.com");
        assertEquals(user, result.block());
    }

    @Test
    void testFindByEmailNotFound() {
        Mockito.when(userRepository.findByEmail("notfound@example.com")).thenReturn(Mono.empty());
        Mono<User> result = userService.findByEmail("notfound@example.com");
        assertThrows(ResourceNotFoundException.class, () -> result.block());
    }

    @Test
    void testChangePasswordSuccess() {
        UUID id = UUID.randomUUID();
        User user = new User();
        user.setId(id);
        user.setPassword("old");
        Mockito.when(userRepository.findById(id)).thenReturn(Mono.just(user));
        Mockito.when(userRepository.update(Mockito.any(User.class))).thenReturn(Mono.just(user));
        Mockito.when(notificationClientService.sendPasswordResetApprovalNotification(id, user.getEmail())).thenReturn(Mono.empty());
        Mono<Void> result = userService.changePassword(id, "new");
        assertDoesNotThrow(() -> result.block());
    }


    @Test
    void testGetAllUsers() {
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        User user2 = new User();
        user2.setId(UUID.randomUUID());
        Mockito.when(userRepository.findAll()).thenReturn(Flux.just(user1, user2));
        Mono<List<User>> result = userService.getAllUsers();
        List<User> users = result.block();
        assertEquals(2, users.size());
    }

    @Test
    void testCreateAddressSuccess() {
        Address address = new Address();
        address.setId(UUID.randomUUID());
        Mockito.when(addressRepository.save(address)).thenReturn(Mono.just(address));
        Mono<Address> result = userService.createAddress(address);
        assertEquals(address, result.block());
    }

    @Test
    void testGetAddressByIdNotFound() {
        UUID id = UUID.randomUUID();
        Mockito.when(addressRepository.findById(id)).thenReturn(Mono.empty());
        Mono<Address> result = userService.getAddressById(id);
        Exception ex = assertThrows(DatabaseException.class, result::block);
        assertTrue(ex.getCause() instanceof ResourceNotFoundException);
    }
} 