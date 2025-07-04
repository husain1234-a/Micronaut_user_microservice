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
            @Client("http://localhost:8080") HttpClient httpClient) {
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
    public Mono<User> createUser(User user) {
        return userRepository.findByEmail(user.getEmail())
            .flatMap(existingUser -> Mono.<User>error(new DatabaseException("User with email " + user.getEmail() + " already exists")))
            .switchIfEmpty(Mono.defer(() -> {
                if (user.getAddress() != null) {
                    // Address logic should be made reactive if AddressRepository is reactive
                    // For now, assume address is set directly
                }
                return userRepository.save(user)
                    .doOnSuccess(savedUser -> notificationClientService.sendUserCreationNotification(savedUser));
            }));
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
    public Mono<Void> deleteUser(UUID id) {
        return getUserById(id)
            .flatMap(user -> userRepository.deleteById(user.getId())
                .then(Mono.fromRunnable(() -> notificationClientService.sendAccountDeletionNotification(user.getId(), user.getEmail()))));
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
                    .doOnSuccess(u -> notificationClientService.sendPasswordResetApprovalNotification(user.getId(), user.getEmail()));
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
                                .doOnSuccess(pcr -> notificationClientService.sendPasswordResetRequestNotification(user.getId(), user.getEmail()))
                                .then();
                        });
                })
            );
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
                    .flatMap(user -> passwordChangeRequestRepository.findByUserIdAndStatus(id, PasswordChangeStatus.PENDING)
                        .flatMap(passwordChangeRequest -> {
                            if (request.isApproved()) {
                                return changePassword(id, passwordChangeRequest.getNewPassword())
                                    .then(Mono.defer(() -> {
                                        passwordChangeRequest.setStatus(PasswordChangeStatus.APPROVED);
                                        passwordChangeRequest.setAdminId(request.getAdminId());
                                        passwordChangeRequest.setUpdatedAt(LocalDateTime.now());
                                        return passwordChangeRequestRepository.update(passwordChangeRequest)
                                            .doOnSuccess(pcr -> notificationClientService.sendPasswordResetApprovalNotification(user.getId(), user.getEmail()))
                                            .then();
                                    }));
                            } else {
                                return rejectPasswordChange(id, request.getAdminId())
                                    .then(Mono.defer(() -> {
                                        passwordChangeRequest.setStatus(PasswordChangeStatus.REJECTED);
                                        passwordChangeRequest.setAdminId(request.getAdminId());
                                        passwordChangeRequest.setUpdatedAt(LocalDateTime.now());
                                        return passwordChangeRequestRepository.update(passwordChangeRequest)
                                            .doOnSuccess(pcr -> notificationClientService.sendPasswordChangeRejectionNotification(user.getId(), user.getEmail()))
                                            .then();
                                    }));
                            }
                        })
                    );
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
                    return passwordChangeRequestRepository.findByUserIdAndStatus(userId, PasswordChangeStatus.PENDING)
                        .flatMap(request -> {
                            request.setStatus(PasswordChangeStatus.REJECTED);
                            request.setAdminId(adminId);
                            request.setUpdatedAt(LocalDateTime.now());
                            return passwordChangeRequestRepository.update(request)
                                .doOnSuccess(pcr -> notificationClientService.sendPasswordChangeRejectionNotification(user.getId(), user.getEmail()))
                                .then();
                        });
                })
            );
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
                .onErrorResume(e -> Mono.just(new HashMap<>()))
            );
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
            .map(user -> currentPassword.equals(user.getPassword())); // Replace with proper password hashing in production
    }

    @Override
    @Transactional
    public Mono<Void> registerFcmToken(String token, String userEmail) {
        return userRepository.findByEmail(userEmail)
            .flatMap(user -> {
                // Implement FCM token registration logic here
                return Mono.empty();
            });
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
    public Mono<Page<User>> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
}