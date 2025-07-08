package com.yash.usermanagement.service;

import java.util.Map;
import java.util.UUID;

import com.yash.usermanagement.dto.UserDeviceDto;
import com.yash.usermanagement.model.User;

import com.yash.usermanagement.dto.PasswordChangeApprovalDTO;
import com.yash.usermanagement.dto.PasswordChangeRequestDTO;
import com.yash.usermanagement.model.PasswordChangeRequest;
import com.yash.usermanagement.model.UserDevice;
import com.yash.usermanagement.model.Address;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

public interface UserService {
    Mono<User> createUser(User user);

    // List<User> getAllUsers();

    Flux<User> getAllUsers();

    Mono<Page<User>> getAllUsers(Pageable pageable);

    Mono<User> getUserById(UUID id);

    Mono<User> updateUser(UUID id, User user);

    Mono<Void> deleteUser(UUID id, String authorization);

    Mono<User> findByEmail(String email);

    Mono<User> getUserByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    Mono<Void> changePassword(UUID userId, String newPassword);

    Mono<Boolean> validateCurrentPassword(UUID userId, String currentPassword);

    Mono<Void> requestPasswordChange(UUID userId, PasswordChangeRequestDTO request);

    Mono<Void> approvePasswordChange(UUID userId, PasswordChangeApprovalDTO request);

    Mono<Void> rejectPasswordChange(UUID userId, UUID adminId);

    Mono<Void> approveOrRejectPasswordChangeRequest(UUID requestId, PasswordChangeApprovalDTO approvalDTO);

    Flux<PasswordChangeRequest> getPendingPasswordChangeRequests();

    Mono<PasswordChangeRequest> getPasswordChangeRequestByUserId(UUID userId);

    Mono<Void> registerFcmToken(String token, String userEmail);

    Flux<Map<String, Object>> getAllPendingPasswordChangeRequests();

    Flux<UserDevice> getUserDevices(UUID userId);

    // Address management methods (moved from AddressService)
    Mono<Address> createAddress(Address address);

    Mono<Address> getAddressById(UUID id);

    Mono<Address> updateAddress(UUID id, Address address);

    Mono<Void> deleteAddress(UUID id);

    <T> Mono<T> sendUserCreationNotification(User savedUser, String authorization);
}