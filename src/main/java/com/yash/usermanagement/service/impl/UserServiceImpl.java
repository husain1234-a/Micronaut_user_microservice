package com.yash.usermanagement.service.impl;

import com.yash.usermanagement.dto.UserDeviceDto;
import com.yash.usermanagement.model.*;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.scheduling.TaskExecutors;

import com.yash.usermanagement.exception.DatabaseException;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.exception.ValidationException;
import com.yash.usermanagement.exception.NotificationFailedException;
import com.yash.usermanagement.repository.AddressRepository;
import com.yash.usermanagement.repository.UserRepository;
import com.yash.usermanagement.repository.PasswordChangeRequestRepository;
import com.yash.usermanagement.repository.UserDeviceRepository;
import com.yash.usermanagement.service.NotificationClientService;
import com.yash.usermanagement.service.UserService;
import com.yash.usermanagement.aop.Loggable;
import com.yash.usermanagement.aop.Auditable;
import com.yash.usermanagement.aop.Timed;
import com.yash.usermanagement.dto.PasswordChangeApprovalDTO;
import com.yash.usermanagement.dto.PasswordChangeRequestDTO;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.cache.annotation.CacheInvalidate;

@Singleton
public class UserServiceImpl implements UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordChangeRequestRepository passwordChangeRequestRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final HttpClient httpClient;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    @Inject
    public UserServiceImpl(UserRepository userRepository, AddressRepository addressRepository,
            PasswordChangeRequestRepository passwordChangeRequestRepository, UserDeviceRepository userDeviceRepository,
            @Client("http://localhost:8081") HttpClient httpClient) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.passwordChangeRequestRepository = passwordChangeRequestRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.httpClient = httpClient;
    }

    @Inject
    private NotificationClientService notificationClientService;

    @Override
    @Loggable
    @Auditable
    @Timed
    @ExecuteOn(TaskExecutors.BLOCKING)
    @CacheInvalidate("users-all")
    public Mono<User> createUser(User user) {
        return userRepository.findByEmail(user.getEmail())
                .flatMap(existingUser -> Mono
                        .<User>error(new DatabaseException("User with email " + user.getEmail() + " already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    if (user.getAddress() != null) {
                        // Address logic should be made reactive if AddressRepository is reactive
                        // For now, assume address is set directly
                    }
                    return userRepository.save(user);
                }));
    }

    @Override
    public Mono<Void> sendUserCreationNotification(User user, String authorization) {
        return notificationClientService.sendUserCreationNotification(user, authorization)
                .onErrorResume(e -> {
                    LOG.error("Notification failed for user creation: {}", e.getMessage(), e);
                    return Mono.error(
                            new NotificationFailedException("User created, but notification could not be sent.", e));
                });
    }

    @Override
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> getUserById(UUID id) {
        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + id)));
    }

    @Override
    @CacheInvalidate("users-all")
    public Mono<User> updateUser(UUID id, User userDetails) {
        return getUserById(id).flatMap(existingUser -> {
            // Address logic should be made reactive if AddressRepository is reactive
            existingUser.setFirstName(userDetails.getFirstName());
            existingUser.setLastName(userDetails.getLastName());
            existingUser.setEmail(userDetails.getEmail());
            existingUser.setGender(userDetails.getGender());
            existingUser.setDateOfBirth(userDetails.getDateOfBirth());
            existingUser.setPhoneNumber(userDetails.getPhoneNumber());
            existingUser.setRole(userDetails.getRole());
            return userRepository.update(existingUser);
        });
    }

    @Override
    @CacheInvalidate("users-all")
    public Mono<Void> deleteUser(UUID id, String authorization) {
        return getUserById(id)
                // .flatMap(user ->
                // notificationClientService.sendAccountDeletionNotification(user.getId(),
                // user.getEmail(), authorization)
                // .onErrorResume(e -> {
                // LOG.error("Notification failed for user deletion: {}", e.getMessage(), e);
                // // Continue with deletion even if notification fails
                // return Mono.empty();
                // })
                // .then(userRepository.deleteById(user.getId()).then())
                // )
                .flatMap(user -> userRepository.deleteById(user.getId()).then());
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with email: " + email)));
    }

    @Override
    public Mono<User> getUserByEmail(String email) {
        return findByEmail(email);
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    public Mono<Void> changePassword(UUID userId, String newPassword) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setPassword(newPassword);
                    return userRepository.update(user)
                            .flatMap(u -> notificationClientService.sendPasswordResetApprovalNotification(user.getId(),
                                    user.getEmail()));
                })
                .then();
    }

    @Override
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    public Mono<Void> requestPasswordChange(UUID id, PasswordChangeRequestDTO request) {
        return getUserById(id)
                .flatMap(user -> userRepository.existsById(id)
                        .flatMap(exists -> {
                            if (!exists) {
                                return Mono.error(new ResourceNotFoundException("User not found with ID: " + id));
                            }
                            return validateCurrentPassword(id, request.getOldPassword())
                                    .flatMap(isValid -> {
                                        if (!isValid) {
                                            return Mono.error(new ValidationException("Current password is incorrect"));
                                        }
                                        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest();
                                        passwordChangeRequest.setUserId(id);
                                        passwordChangeRequest.setNewPassword(request.getNewPassword());
                                        passwordChangeRequest.setStatus(PasswordChangeStatus.PENDING);
                                        passwordChangeRequest.setCreatedAt(LocalDateTime.now());
                                        return passwordChangeRequestRepository.save(passwordChangeRequest)
                                                .doOnSuccess(pcr -> notificationClientService
                                                        .sendPasswordResetRequestNotification(user.getId(),
                                                                user.getEmail()))
                                                .then();
                                    });
                        }));
    }

    @Override
    @Transactional
    public Mono<Void> approvePasswordChange(UUID id, PasswordChangeApprovalDTO request) {
        return getUserById(request.getAdminId())
                .flatMap(admin -> {
                    if (admin.getRole() != UserRole.ADMIN) {
                        return Mono.error(new ValidationException("Only admin can approve password changes"));
                    }
                    return getUserById(id)
                            .flatMap(user -> passwordChangeRequestRepository
                                    .findByUserIdAndStatus(id, PasswordChangeStatus.PENDING)
                                    .flatMap(passwordChangeRequest -> {
                                        if (request.isApproved()) {
                                            return changePassword(id, passwordChangeRequest.getNewPassword())
                                                    .then(Mono.defer(() -> {
                                                        passwordChangeRequest.setStatus(PasswordChangeStatus.APPROVED);
                                                        passwordChangeRequest.setAdminId(request.getAdminId());
                                                        passwordChangeRequest.setUpdatedAt(LocalDateTime.now());
                                                        return passwordChangeRequestRepository
                                                                .update(passwordChangeRequest)
                                                                .flatMap(pcr -> notificationClientService
                                                                        .sendPasswordResetApprovalNotification(
                                                                                user.getId(), user.getEmail()))
                                                                .then();
                                                    }));
                                        } else {
                                            return rejectPasswordChange(id, request.getAdminId())
                                                    .then(Mono.defer(() -> {
                                                        passwordChangeRequest.setStatus(PasswordChangeStatus.REJECTED);
                                                        passwordChangeRequest.setAdminId(request.getAdminId());
                                                        passwordChangeRequest.setUpdatedAt(LocalDateTime.now());
                                                        return passwordChangeRequestRepository
                                                                .update(passwordChangeRequest)
                                                                .flatMap(pcr -> notificationClientService
                                                                        .sendPasswordChangeRejectionNotification(
                                                                                user.getId(), user.getEmail()))
                                                                .then();
                                                    }));
                                        }
                                    }));
                });
    }

    @Override
    @ExecuteOn(TaskExecutors.BLOCKING)
    public Mono<Void> rejectPasswordChange(UUID userId, UUID adminId) {
        return getUserById(userId)
                .flatMap(user -> getUserById(adminId)
                        .flatMap(admin -> {
                            if (admin.getRole() != UserRole.ADMIN) {
                                return Mono.error(new ValidationException("Only admin can reject password changes"));
                            }
                            return passwordChangeRequestRepository
                                    .findByUserIdAndStatus(userId, PasswordChangeStatus.PENDING)
                                    .flatMap(request -> {
                                        request.setStatus(PasswordChangeStatus.REJECTED);
                                        request.setAdminId(adminId);
                                        request.setUpdatedAt(LocalDateTime.now());
                                        return passwordChangeRequestRepository.update(request)
                                                .flatMap(pcr -> notificationClientService
                                                        .sendPasswordChangeRejectionNotification(user.getId(),
                                                                user.getEmail()))
                                                .then();
                                    });
                        }));
    }

    @Override
    public Flux<PasswordChangeRequest> getPendingPasswordChangeRequests() {
        return passwordChangeRequestRepository.findByStatus(PasswordChangeStatus.PENDING);
    }

    @Override
    public Flux<Map<String, Object>> getAllPendingPasswordChangeRequests() {
        return passwordChangeRequestRepository.findByStatus(PasswordChangeStatus.PENDING)
                .flatMap(request -> getUserById(request.getUserId())
                        .map(user -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", request.getId());
                            map.put("userId", request.getUserId());
                            map.put("newPassword", request.getNewPassword());
                            map.put("status", request.getStatus());
                            map.put("adminId", request.getAdminId());
                            map.put("createdAt", request.getCreatedAt());
                            map.put("updatedAt", request.getUpdatedAt());
                            map.put("userFirstName", user.getFirstName());
                            map.put("userLastName", user.getLastName());
                            map.put("userEmail", user.getEmail());
                            return map;
                        })
                        .onErrorResume(e -> Mono.just(new HashMap<>())));
    }

    @Override
    public Flux<UserDevice> getUserDevices(UUID userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    @Override
    public Mono<PasswordChangeRequest> getPasswordChangeRequestByUserId(UUID userId) {
        return passwordChangeRequestRepository.findByUserIdAndStatus(userId, PasswordChangeStatus.PENDING);
    }

    @Override
    public Mono<Boolean> validateCurrentPassword(UUID userId, String currentPassword) {
        return getUserById(userId)
                .map(user -> currentPassword.equals(user.getPassword())); // Replace with proper password hashing in
                                                                          // production
    }

    @Override
    @Transactional
    public Mono<Void> registerFcmToken(String token, String userEmail) {
        return userRepository.findByEmail(userEmail)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with email: " + userEmail)))
                .flatMap(user -> userDeviceRepository.findByUserId(user.getId())
                        .filter(device -> token.equals(device.getFcmToken()))
                        .hasElements()
                        .flatMap(alreadyRegistered -> {
                            if (alreadyRegistered) {
                                LOG.info("Token already registered for user: {}", userEmail);
                                return Mono.empty();
                            }
                            UserDevice userDevice = new UserDevice();
                            userDevice.setUserId(user.getId());
                            userDevice.setFcmToken(token);
                            userDevice.setCreatedAt(LocalDateTime.now());
                            return userDeviceRepository.save(userDevice).then();
                        }));
    }

    @Override
    @ExecuteOn(TaskExecutors.BLOCKING)
    public Mono<Void> approveOrRejectPasswordChangeRequest(UUID requestId, PasswordChangeApprovalDTO approvalDTO) {
        return passwordChangeRequestRepository.findById(requestId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Password change request not found")))
                .flatMap(request -> {
                    if (approvalDTO.isApproved()) {
                        return approvePasswordChange(request.getUserId(), approvalDTO);
                    } else {
                        return rejectPasswordChange(request.getUserId(), approvalDTO.getAdminId());
                    }
                });
    }

    @Override
    @Cacheable("users-all")
    public Mono<Page<User>> getAllUsers(Pageable pageable) {
        LOG.info("Fetching users from DB...");
        return userRepository.findAll(pageable);
    }

    // Address management methods (moved from AddressServiceImpl)
    @Override
    public Mono<Address> createAddress(Address address) {
        LOG.info("Creating new address");
        return addressRepository.save(address)
                .doOnSuccess(savedAddress -> LOG.info("Address created successfully with ID: {}", savedAddress.getId()))
                .onErrorMap(e -> {
                    LOG.error("Error creating address: {}", e.getMessage(), e);
                    return new DatabaseException("Failed to create address", e);
                });
    }

    @Override
    public Mono<Address> getAddressById(UUID id) {
        LOG.info("Fetching address with ID: {}", id);
        return addressRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Address not found with ID: " + id)))
                .doOnSuccess(address -> LOG.info("Address found with ID: {}", id))
                .onErrorMap(e -> {
                    LOG.error("Error fetching address with ID {}: {}", id, e.getMessage(), e);
                    return new DatabaseException("Failed to fetch address", e);
                });
    }

    @Override
    public Mono<Address> updateAddress(UUID id, Address address) {
        LOG.info("Updating address with ID: {}", id);
        return addressRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Address not found with ID: " + id)))
                .flatMap(existingAddress -> {
                    address.setId(id);
                    return addressRepository.update(address)
                            .doOnSuccess(updatedAddress -> LOG.info("Address updated successfully with ID: {}", id));
                })
                .onErrorMap(e -> {
                    LOG.error("Error updating address with ID {}: {}", id, e.getMessage(), e);
                    return new DatabaseException("Failed to update address", e);
                });
    }

    @Override
    public Mono<Void> deleteAddress(UUID id) {
        LOG.info("Deleting address with ID: {}", id);
        return addressRepository.existsById(id)
                .flatMap(exists -> {
                    if (exists) {
                        return addressRepository.deleteById(id)
                                .doOnSuccess(v -> LOG.info("Address deleted successfully with ID: {}", id))
                                .then();
                    } else {
                        LOG.warn("Address not found with ID: {}", id);
                        return Mono.error(new ResourceNotFoundException("Address not found with ID: " + id));
                    }
                })
                .onErrorMap(e -> {
                    LOG.error("Error deleting address with ID {}: {}", id, e.getMessage(), e);
                    return new DatabaseException("Failed to delete address", e);
                });
    }
}