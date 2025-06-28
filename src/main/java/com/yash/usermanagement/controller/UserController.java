package com.yash.usermanagement.controller;

import com.yash.usermanagement.model.User;
import com.yash.usermanagement.service.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yash.usermanagement.dto.CreateUserRequest;
import com.yash.usermanagement.dto.UpdateUserRequest;
import com.yash.usermanagement.dto.UserResponse;
import com.yash.usermanagement.dto.PasswordChangeRequestDTO;
import com.yash.usermanagement.dto.PasswordChangeApprovalDTO;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.exception.ValidationException;
import com.yash.usermanagement.exception.DuplicateResourceException;
import com.yash.usermanagement.model.PasswordChangeRequest;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/api/users")
@Tag(name = "User Management")
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    

    @Post
    @Operation(summary = "Create a new user")
    @Secured("ADMIN")
    @ExecuteOn(TaskExecutors.BLOCKING)  
    public HttpResponse<UserResponse> createUser(@Body @Valid CreateUserRequest request) {
        LOG.info("Creating new user with role: {}", request.getRole());
        try {
            User user = convertToUser(request);
            User createdUser = userService.createUser(user);

            return HttpResponse.created(convertToUserResponse(createdUser));
        } catch (DuplicateResourceException e) {
            LOG.warn("Duplicate user creation attempted: {}", e.getMessage());
            throw e;
        } catch (ValidationException e) {
            LOG.warn("Invalid user data: {}", e.getMessage());
            throw e;
        }
    }

    @Get
    @Operation(summary = "Get all users")
    @Secured("ADMIN")
    public HttpResponse<List<UserResponse>> getAllUsers() {
        LOG.info("Fetching all users");
        List<User> users = userService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
        return HttpResponse.ok(userResponses);
    }

    @Get("/{id}")
    @Operation(summary = "Get user by ID")
    @Secured({ "ADMIN", "USER" })
    public HttpResponse<UserResponse> getUserById(@PathVariable UUID id) {
        LOG.info("Fetching user with id: {}", id);
        try {
            User user = userService.getUserById(id);
            return HttpResponse.ok(convertToUserResponse(user));
        } catch (ResourceNotFoundException e) {
            LOG.warn("User not found with id: {}", id);
            throw e;
        }
    }

    @Put("/{id}")
    @Operation(summary = "Update user")
    @Secured({ "ADMIN", "USER" })
    public HttpResponse<UserResponse> updateUser(@PathVariable UUID id, @Body @Valid UpdateUserRequest request) {
        try {
            User user = convertToUser(request);
            User updatedUser = userService.updateUser(id, user);
            return HttpResponse.ok(convertToUserResponse(updatedUser));
        } catch (Exception e) {
            LOG.error("Error updating user: {}", e.getMessage());
            throw e;
        }
    }

     @Delete("/{id}")
    @Operation(summary = "Delete user")
    @Secured({ "ADMIN", "USER" })
    public MutableHttpResponse<Map<String, Boolean>> deleteUser(@PathVariable UUID id) {
        LOG.info("Deleting user with id: {}", id);
        userService.deleteUser(id);
        return HttpResponse.ok(Collections.singletonMap("success", true));
    }

    @Get("/email/{email}")
    @Operation(summary = "Get user by email")
    @Secured({ "ADMIN", "USER" })
    public HttpResponse<UserResponse> getUserByEmail(@PathVariable String email) {
        LOG.info("Finding user by email: {}", email);
        try {
            return userService.findByEmail(email)
                    .map(this::convertToUserResponse)
                    .map(HttpResponse::ok)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        } catch (ValidationException e) {
            LOG.warn("Invalid email format: {}", email);
            throw e;
        }
    }

        @Post("/{id}/change-password")
    @Operation(summary = "Request password change")
    @Secured("USER")
    public HttpResponse<Void> requestPasswordChange(
            @PathVariable UUID id,
            @Body @Valid PasswordChangeRequestDTO request) {
        LOG.info("Requesting password change for user with id: {}", id);
        
        // Now notification to admin handled by userService
        try{
            userService.requestPasswordChange(id, request);
            return HttpResponse.accepted();
        }catch (Exception e){
            LOG.error("Failed to request password change: {}", e.getMessage());
            return HttpResponse.serverError();
        }

    }

    @Put("/{id}/approve-password-change")
    @Operation(summary = "Approve password change request")
    @Secured("ADMIN")
    public HttpResponse<Void> approvePasswordChange(
            @PathVariable UUID id,
            @Body @Valid PasswordChangeApprovalDTO request) {
        LOG.info("Processing password change approval for user with id: {}", id);
        try {
            
            userService.approvePasswordChange(id, request);
            return HttpResponse.ok();
        } catch (ResourceNotFoundException e) {
            LOG.warn("User not found for password change approval with id: {}", id);
            throw e;
        }
    }

    @Get("/password-change-requests/pending")
    @Secured("ADMIN")
    @Operation(summary = "Get all pending password change requests")
    public HttpResponse<List<Map<String, Object>>> getAllPendingPasswordChangeRequests() {
        List<PasswordChangeRequest> pendingRequests = userService.getPendingPasswordChangeRequests();
        // For each request, fetch user info for display
        try{
            List<Map<String, Object>> result = userService.getAllPendingPasswordChangeRequests();
        return HttpResponse.ok(result);
    }catch (Exception e) {
            LOG.error("Failed to fetch pending password change requests: {}", e.getMessage());
            return HttpResponse.serverError();
        }

    }

    @Put("/password-change-requests/{requestId}/approve")
    @Secured("ADMIN")
    @Operation(summary = "Approve or reject a password change request")
    public HttpResponse<Void> approveOrRejectPasswordChangeRequest(
            @PathVariable UUID requestId,
            @Body @Valid PasswordChangeApprovalDTO approvalDTO) {
        // Find the request
        try{
            userService.approveOrRejectPasswordChangeRequest(requestId, approvalDTO);
            return HttpResponse.ok();
        }catch (Exception e) {
            LOG.error("Failed to process password change request: {}", e.getMessage());
            return HttpResponse.serverError();
        }    
    }

    // Helper methods for conversion
    private User convertToUser(CreateUserRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setAddress(request.getAddress());
        return user;
    }

    private User convertToUser(UpdateUserRequest request) {
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setRole(request.getRole());
        user.setAddress(request.getAddress());
        return user;
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setDateOfBirth(user.getDateOfBirth());
        response.setAddress(user.getAddress());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setGender(user.getGender());
        response.setRole(user.getRole());
        return response;
    }
}