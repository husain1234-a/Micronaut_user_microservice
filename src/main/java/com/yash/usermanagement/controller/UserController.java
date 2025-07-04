package com.yash.usermanagement.controller;

import com.yash.usermanagement.dto.*;
import com.yash.usermanagement.model.User;
import com.yash.usermanagement.model.UserDevice;
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
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.exception.ValidationException;
import com.yash.usermanagement.exception.DuplicateResourceException;
import com.yash.usermanagement.model.PasswordChangeRequest;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import java.util.*;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

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
    public Mono<HttpResponse<UserResponse>> createUser(@Body @Valid CreateUserRequest request) {
        LOG.info("Creating new user with role: {}", request.getRole());
        User user = convertToUser(request);
        return userService.createUser(user)
            .map(this::convertToUserResponse)
            .map(HttpResponse::created);
    }

    @Get
    @Operation(summary = "Get all users (paginated)")
    @Secured("ADMIN")
    public Mono<Page<UserResponse>> getAllUsers(@QueryValue(defaultValue = "0") int page,
                                                @QueryValue(defaultValue = "2") int size) {
        Pageable pageable = Pageable.from(page, size);
        return userService.getAllUsers(pageable)
            .map(userPage -> userPage.map(this::convertToUserResponse));
    }

    @Get("/{id}")
    @Operation(summary = "Get user by ID")
    @Secured({ "ADMIN", "USER" })
    public Mono<HttpResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        LOG.info("Fetching user with id: {}", id);
        return userService.getUserById(id)
            .map(this::convertToUserResponse)
            .map(HttpResponse::ok);
    }

    @Put("/{id}")
    @Operation(summary = "Update user")
    @Secured({ "ADMIN", "USER" })
    public Mono<HttpResponse<UserResponse>> updateUser(@PathVariable UUID id, @Body @Valid UpdateUserRequest request) {
        User user = convertToUser(request);
        return userService.updateUser(id, user)
            .map(this::convertToUserResponse)
            .map(HttpResponse::ok);
    }

    @Delete("/{id}")
    @Operation(summary = "Delete user")
    @Secured({ "ADMIN", "USER" })
    public Mono<MutableHttpResponse<Map<String, Boolean>>> deleteUser(@PathVariable UUID id) {
        LOG.info("Deleting user with id: {}", id);
        return userService.deleteUser(id)
            .thenReturn(HttpResponse.ok(Collections.singletonMap("success", true)));
    }

    @Get("/email/{email}")
    @Operation(summary = "Get user by email")
    @Secured({ "ADMIN", "USER" })
    public Mono<HttpResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        LOG.info("Finding user by email: {}", email);
        return userService.findByEmail(email)
            .map(this::convertToUserResponse)
            .map(HttpResponse::ok);
    }

    @Post("/{id}/change-password")
    @Operation(summary = "Request password change")
    @Secured("USER")
    public Mono<HttpResponse<Void>> requestPasswordChange(
            @PathVariable UUID id,
            @Body @Valid PasswordChangeRequestDTO request) {
        LOG.info("Requesting password change for user with id: {}", id);
        return userService.requestPasswordChange(id, request)
            .thenReturn(HttpResponse.accepted());
    }

    @Put("/{id}/approve-password-change")
    @Operation(summary = "Approve password change request")
    @Secured("ADMIN")
    public Mono<HttpResponse<Void>> approvePasswordChange(
            @PathVariable UUID id,
            @Body @Valid PasswordChangeApprovalDTO request) {
        LOG.info("Processing password change approval for user with id: {}", id);
        return userService.approvePasswordChange(id, request)
            .thenReturn(HttpResponse.ok());
    }

    @Get("/password-change-requests/pending")
    @Secured("ADMIN")
    @Operation(summary = "Get all pending password change requests")
    public Flux<Map<String, Object>> getAllPendingPasswordChangeRequests() {
        return userService.getAllPendingPasswordChangeRequests();
    }

    @Put("/password-change-requests/{requestId}/approve")
    @Secured("ADMIN")
    @Operation(summary = "Approve or reject a password change request")
    public Mono<HttpResponse<Void>> approveOrRejectPasswordChangeRequest(
            @PathVariable UUID requestId,
            @Body @Valid PasswordChangeApprovalDTO approvalDTO) {
        return userService.approveOrRejectPasswordChangeRequest(requestId, approvalDTO)
            .thenReturn(HttpResponse.ok());
    }

    @Get("/{userId}/devices")
    @Operation(summary = "Get devices by userId")
    @Secured({ "ADMIN", "USER" })
    public Flux<UserDevice> getUserDevices(@PathVariable UUID userId) {
        LOG.info("Finding devices by userId: {}", userId);
        return userService.getUserDevices(userId);
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