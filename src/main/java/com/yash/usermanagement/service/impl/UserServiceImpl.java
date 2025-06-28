package com.yash.usermanagement.service.impl;

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
    public User createUser(User user) {
        try {
            LOG.info("Creating new user with email: {}", user.getEmail());

            // Check if user with email already exists
            Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
            if (existingUser.isPresent()) {
                throw new DatabaseException("User with email " + user.getEmail() + " already exists");
            }

            // First create address if present
            if (user.getAddress() != null) {
                Address address = user.getAddress();
                Address savedAddress = addressRepository.save(address);
                user.setAddress(savedAddress);
            }

            User savedUser = userRepository.save(user);
            LOG.info("User created successfully with ID: {}", savedUser.getId());

            // Send notification via Notification microservice
            notificationClientService.sendUserCreationNotification(savedUser.getEmail(),
                    "Welcome " + savedUser.getFirstName() + "!");

            return savedUser;
        } catch (Exception e) {
            LOG.error("Error creating user: {}", e.getMessage());
            throw new DatabaseException("Failed to create user", e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        try {
            LOG.info("Fetching all users");
            return userRepository.findAll();
        } catch (Exception e) {
            LOG.error("Error fetching users: {}", e.getMessage());
            throw new DatabaseException("Failed to fetch users", e);
        }
    }

    @Override
    public User getUserById(UUID id) {
        try {
            LOG.info("Fetching user with id: {}", id);
            return userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error fetching user: {}", e.getMessage());
            throw new DatabaseException("Failed to fetch user", e);
        }
    }

    @Override
    @Loggable
    @Auditable
    @Timed
    public User updateUser(UUID id, User userDetails) {
        try {
            LOG.info("Updating user with id: {}", id);
            User existingUser = getUserById(id);

            // Update address if present
            if (userDetails.getAddress() != null) {
                Address address = userDetails.getAddress();
                if (existingUser.getAddress() != null && existingUser.getAddress().getId() != null) {
                    // Update existing address
                    address.setId(existingUser.getAddress().getId());
                    Address updatedAddress = addressRepository.update(address);
                    existingUser.setAddress(updatedAddress);
                } else {
                    // Create new address
                    Address savedAddress = addressRepository.save(address);
                    existingUser.setAddress(savedAddress);
                }
            }

            // Update other user fields
            existingUser.setFirstName(userDetails.getFirstName());
            existingUser.setLastName(userDetails.getLastName());
            existingUser.setEmail(userDetails.getEmail());
            existingUser.setGender(userDetails.getGender());
            existingUser.setDateOfBirth(userDetails.getDateOfBirth());
            existingUser.setPhoneNumber(userDetails.getPhoneNumber());
            existingUser.setRole(userDetails.getRole());

            User updatedUser = userRepository.update(existingUser);
            LOG.info("User updated successfully with ID: {}", id);
            return updatedUser;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error updating user: {}", e.getMessage());
            throw new DatabaseException("Failed to update user", e);
        }
    }

    @Override
    @Loggable
    @Auditable
    @Timed
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void deleteUser(UUID id) {
        try {
            LOG.info("Deleting user with id: {}", id);
            User user = getUserById(id);

            // Delete address if exists
            if (user.getAddress() != null && user.getAddress().getId() != null) {
                addressRepository.deleteById(user.getAddress().getId());
            }

            userRepository.deleteById(id);

            // Send deletion notification
            try {
                notificationClientService.sendAccountDeletionNotification(user.getId(), user.getEmail());
            } catch (Exception e) {
                LOG.error("Failed to send deletion notification email: {}", e.getMessage());
            }

            LOG.info("User deleted successfully with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error deleting user: {}", e.getMessage());
            throw new DatabaseException("Failed to delete user", e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        try {
            LOG.info("Finding user by email: {}", email);
            return userRepository.findByEmail(email);
        } catch (Exception e) {
            LOG.error("Error finding user by email: {}", e.getMessage());
            throw new DatabaseException("Failed to find user by email", e);
        }
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        try {
            LOG.info("Fetching user with email: {}", email);
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isPresent()) {
                LOG.info("User found with email: {}", email);
            } else {
                LOG.warn("User not found with email: {}", email);
            }
            return user;
        } catch (Exception e) {
            LOG.error("Error fetching user with email {}: {}", email, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch user by email", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try {
            LOG.info("Checking if user exists with email: {}", email);
            boolean exists = userRepository.existsByEmail(email);
            LOG.info("User exists with email {}: {}", email, exists);
            return exists;
        } catch (Exception e) {
            LOG.error("Error checking user existence with email {}: {}", email, e.getMessage(), e);
            throw new DatabaseException("Failed to check user existence", e);
        }
    }

    @Override
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void changePassword(UUID userId, String newPassword) {
        try {
            LOG.info("Changing password for user with ID: {}", userId);
            userRepository.findById(userId)
                    .ifPresentOrElse(
                            user -> {
                                user.setPassword(newPassword);
                                userRepository.update(user);
                                LOG.info("Password changed successfully for user with ID: {}", userId);

                                // Send password change approval notification
                                try {
                                    notificationClientService.sendPasswordResetApprovalNotification(user.getId(),
                                            user.getEmail());
                                } catch (Exception e) {
                                    LOG.error("Failed to send password reset approval email: {}", e.getMessage());
                                }
                            },
                            () -> {
                                LOG.warn("User not found with ID: {}", userId);
                                throw new ResourceNotFoundException("User not found with ID: " + userId);
                            });
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error changing password for user with ID {}: {}", userId, e.getMessage(), e);
            throw new DatabaseException("Failed to change password", e);
        }
    }

    @Override
    @Transactional
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void requestPasswordChange(UUID id, PasswordChangeRequestDTO request) {
        try {
            LOG.info("Requesting password change for user with ID: {}", id);
            User user = getUserById(id);
            if (!userRepository.existsById(id)) {
                LOG.warn("User not found with ID: {}", id);
                throw new ResourceNotFoundException("User not found with ID: " + id);
            } else {
                if (!validateCurrentPassword(id, request.getOldPassword())) {
                    throw new ValidationException("Current password is incorrect");
                } else {
                    // Create password change request
                    PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest();
                    passwordChangeRequest.setUserId(id);
                    passwordChangeRequest.setNewPassword(request.getNewPassword());
                    passwordChangeRequest.setStatus(PasswordChangeStatus.PENDING);
                    passwordChangeRequest.setCreatedAt(LocalDateTime.now());
                    passwordChangeRequestRepository.save(passwordChangeRequest);
                    // Send notification to admin
                    try {
                        notificationClientService.sendPasswordResetRequestNotification(user.getId(), user.getEmail());
                    } catch (Exception e) {
                        LOG.error("Failed to send password reset request email: {}", e.getMessage());
                    }
                }
            }

            LOG.info("Password change requested successfully for user with ID: {}", id);

        } catch (Exception e) {
            LOG.error("Error requesting password change for user with ID {}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to request password change", e);
        }
    }

    @Override
    @Transactional
    public void approvePasswordChange(UUID id, PasswordChangeApprovalDTO request) {
        try {
            LOG.info("Approving password change for user with ID: {}", id);
            // Verify admin
            User admin = getUserById(request.getAdminId());
            if (admin.getRole() != UserRole.ADMIN) {
                throw new ValidationException("Only admin can approve password changes");
            }

            // Get user and password change request
            User user = getUserById(id);
            PasswordChangeRequest passwordChangeRequest = passwordChangeRequestRepository
                    .findByUserIdAndStatus(id, PasswordChangeStatus.PENDING)
                    .orElseThrow(() -> new ResourceNotFoundException("No pending password change request found"));

            if (request.isApproved()) {
                // Update password
                changePassword(id, passwordChangeRequest.getNewPassword());

                // Update request status and admin ID
                passwordChangeRequest.setStatus(PasswordChangeStatus.APPROVED);
                passwordChangeRequest.setAdminId(request.getAdminId());
                passwordChangeRequest.setUpdatedAt(LocalDateTime.now());
                passwordChangeRequestRepository.update(passwordChangeRequest);

                // Send approval notification
                try {
                    notificationClientService.sendPasswordResetApprovalNotification(user.getId(), user.getEmail());
                } catch (Exception e) {
                    LOG.error("Failed to send password reset approval email: {}", e.getMessage());
                }
            } else {
                // Reject password change
                rejectPasswordChange(id, request.getAdminId());

                // Update request status and admin ID
                passwordChangeRequest.setStatus(PasswordChangeStatus.REJECTED);
                passwordChangeRequest.setAdminId(request.getAdminId());
                passwordChangeRequest.setUpdatedAt(LocalDateTime.now());
                passwordChangeRequestRepository.update(passwordChangeRequest);

                // Send rejection notification
                try {
                    notificationClientService.sendPasswordChangeRejectionNotification(user.getId(), user.getEmail());
                } catch (Exception e) {
                    LOG.error("Failed to send password change rejection email: {}", e.getMessage());
                }
            }
            if (!userRepository.existsById(id)) {
                LOG.warn("User not found with ID: {}", id);
                throw new ResourceNotFoundException("User not found with ID: " + id);
            }
            // Implementation for approving password change
            LOG.info("Password change approved successfully for user with ID: {}", id);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error approving password change for user with ID {}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to approve password change", e);
        }
    }

    @Override
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void rejectPasswordChange(UUID userId, UUID adminId) {
        LOG.info("Rejecting password change request for user: {}", userId);
        try {
            User user = getUserById(userId);
            User admin = getUserById(adminId);

            if (admin.getRole() != UserRole.ADMIN) {
                throw new ValidationException("Only admin can reject password changes");
            }

            PasswordChangeRequest request = passwordChangeRequestRepository
                    .findByUserIdAndStatus(userId, PasswordChangeStatus.PENDING)
                    .orElseThrow(() -> new ResourceNotFoundException("No pending password change request found"));

            request.setStatus(PasswordChangeStatus.REJECTED);
            request.setAdminId(adminId);
            request.setUpdatedAt(LocalDateTime.now());
            passwordChangeRequestRepository.update(request);

            // Send rejection notification
            try {
                notificationClientService.sendPasswordChangeRejectionNotification(user.getId(), user.getEmail());
            } catch (Exception e) {
                LOG.error("Failed to send password change rejection email: {}", e.getMessage());
            }

            LOG.info("Password change request rejected for user: {}", userId);
        } catch (Exception e) {
            LOG.error("Error rejecting password change request: {}", e.getMessage());
            throw new DatabaseException("Failed to reject password change request", e);
        }
    }

    @Override
    public List<PasswordChangeRequest> getPendingPasswordChangeRequests() {
        LOG.info("Fetching all pending password change requests");
        try {
            return passwordChangeRequestRepository.findByStatus(PasswordChangeStatus.PENDING);
        } catch (Exception e) {
            LOG.error("Error fetching pending password change requests: {}", e.getMessage());
            throw new DatabaseException("Failed to fetch pending password change requests", e);
        }
    }

    @Override
    public List<Map<String, Object>> getAllPendingPasswordChangeRequests() {
        LOG.info("Fetching all pending password change requests");
        try {
            List<PasswordChangeRequest> pendingRequests = passwordChangeRequestRepository
                    .findByStatus(PasswordChangeStatus.PENDING);
            // Map the pending requests to a more suitable format for the response
            return pendingRequests.stream().map(req -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", req.getId());
                map.put("userId", req.getUserId());
                map.put("newPassword", req.getNewPassword());
                map.put("status", req.getStatus());
                map.put("adminId", req.getAdminId());
                map.put("createdAt", req.getCreatedAt());
                map.put("updatedAt", req.getUpdatedAt());
                try {
                    User user = getUserById(req.getUserId());
                    map.put("userFirstName", user.getFirstName());
                    map.put("userLastName", user.getLastName());
                    map.put("userEmail", user.getEmail());
                } catch (Exception e) {
                    // User might have been deleted
                    map.put("userFirstName", "");
                    map.put("userLastName", "");
                    map.put("userEmail", "");
                }
                return map;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            LOG.error("Error fetching all pending password change requests: {}", e.getMessage());
            throw new DatabaseException("Failed to fetch all pending password change requests", e);
        }
    }

    @Override
    public Optional<PasswordChangeRequest> getPasswordChangeRequestByUserId(UUID userId) {
        LOG.info("Fetching password change request for user: {}", userId);
        try {
            return passwordChangeRequestRepository.findByUserIdAndStatus(userId, PasswordChangeStatus.PENDING);
        } catch (Exception e) {
            LOG.error("Error fetching password change request: {}", e.getMessage());
            throw new DatabaseException("Failed to fetch password change request", e);
        }
    }

    @Override
    public boolean validateCurrentPassword(UUID userId, String currentPassword) {
        LOG.info("Validating current password for user with ID: {}", userId);
        try {
            User user = getUserById(userId);
            // Here you should use your password hashing mechanism to compare passwords
            // For example, if using BCrypt:
            // return BCrypt.checkpw(currentPassword, user.getPassword());
            return currentPassword.equals(user.getPassword()); // This is just for example, use proper password hashing
                                                               // in production
        } catch (Exception e) {
            LOG.error("Error validating current password for user with ID {}: {}", userId, e.getMessage(), e);
            throw new DatabaseException("Failed to validate current password", e);
        }
    }

    @Override
    @Transactional
    public void registerFcmToken(String token, String userEmail) {
        LOG.info("Registering FCM token for user: {}", userEmail);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Check if this token is already registered for this user
        List<UserDevice> existingDevices = userDeviceRepository.findByUserId(user.getId());
        boolean alreadyRegistered = existingDevices.stream()
                .anyMatch(device -> device.getFcmToken().equals(token));
        if (alreadyRegistered) {
            LOG.info("Token already registered for user: {}", userEmail);
            return;
        }

        UserDevice userDevice = new UserDevice();
        userDevice.setUserId(user.getId());
        userDevice.setFcmToken(token);
        userDevice.setCreatedAt(LocalDateTime.now());

        userDeviceRepository.save(userDevice);
        LOG.info("Successfully registered FCM token for user {}", userEmail);
    }

    @Override
    @ExecuteOn(TaskExecutors.BLOCKING)
    public void approveOrRejectPasswordChangeRequest(UUID requestId, PasswordChangeApprovalDTO approvalDTO) {
        LOG.info("Processing password change request approval for request ID: {}", requestId);
        PasswordChangeRequest req = passwordChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Password change request not found"));
        if (approvalDTO.isApproved()) {
            // Approve: change password, set status, set adminId, set updatedAt
            changePassword(req.getUserId(), req.getNewPassword());
            req.setStatus(PasswordChangeStatus.APPROVED);
            req.setAdminId(approvalDTO.getAdminId());
            req.setUpdatedAt(LocalDateTime.now());
            passwordChangeRequestRepository.update(req);
            // Send approval notification

            User user = getUserById(req.getUserId());
            notificationClientService.sendPasswordResetApprovalNotification(user.getId(), user.getEmail());

        } else {
            // Reject: set status, set adminId, set updatedAt
            req.setStatus(PasswordChangeStatus.REJECTED);
            req.setAdminId(approvalDTO.getAdminId());
            req.setUpdatedAt(LocalDateTime.now());
            passwordChangeRequestRepository.update(req);
            // Send rejection notification

            User user = getUserById(req.getUserId());
            notificationClientService.sendPasswordChangeRejectionNotification(user.getId(), user.getEmail());

        }
    }
}