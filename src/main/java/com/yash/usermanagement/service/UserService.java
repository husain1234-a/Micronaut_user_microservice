package com.yash.usermanagement.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.yash.usermanagement.dto.UserDeviceDto;
import com.yash.usermanagement.model.User;

import com.yash.usermanagement.dto.PasswordChangeApprovalDTO;
import com.yash.usermanagement.dto.PasswordChangeRequestDTO;
import com.yash.usermanagement.model.PasswordChangeRequest;
import com.yash.usermanagement.model.UserDevice;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

public interface UserService {
    User createUser(User user);

    // List<User> getAllUsers();

    Page<User> getAllUsers(Pageable pageable);

    User getUserById(UUID id);

    User updateUser(UUID id, User user);

    void deleteUser(UUID id);

    Optional<User> findByEmail(String email);

    Optional<User> getUserByEmail(String email);

    boolean existsByEmail(String email);

    void changePassword(UUID userId, String newPassword);

    boolean validateCurrentPassword(UUID userId, String currentPassword);

    void requestPasswordChange(UUID userId, PasswordChangeRequestDTO request);

    void approvePasswordChange(UUID userId, PasswordChangeApprovalDTO request);

    void rejectPasswordChange(UUID userId, UUID adminId);
   
    void approveOrRejectPasswordChangeRequest(UUID requestId, PasswordChangeApprovalDTO approvalDTO);

    List<PasswordChangeRequest> getPendingPasswordChangeRequests();

    Optional<PasswordChangeRequest> getPasswordChangeRequestByUserId(UUID userId);

    void registerFcmToken(String token, String userEmail);

    List<Map<String, Object>> getAllPendingPasswordChangeRequests();

    List<UserDevice> getUserDevices(UUID userId);
}