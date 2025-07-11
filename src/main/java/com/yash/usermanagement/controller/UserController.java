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
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;

import com.yash.usermanagement.dto.AddressResponse;
import com.yash.usermanagement.dto.CreateAddressRequest;
import com.yash.usermanagement.dto.UpdateAddressRequest;
import com.yash.usermanagement.model.Address;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.exception.ValidationException;
import com.yash.usermanagement.exception.NotificationFailedException;
import com.yash.usermanagement.dto.UserCreationResponse;
import io.micronaut.http.HttpHeaders;
import reactor.core.publisher.Mono;

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
    public Mono<HttpResponse<UserCreationResponse>> createUser(@Body @Valid CreateUserRequest request,
            @Header(HttpHeaders.AUTHORIZATION) String authorization) {
        LOG.info("Creating new user with role: {}", request.getRole());
        User user = convertToUser(request);
        return userService.createUser(user)
                .flatMap(savedUser -> userService.sendUserCreationNotification(savedUser, authorization)
                        .thenReturn(
                                HttpResponse.created(new UserCreationResponse(convertToUserResponse(savedUser), null)))
                        .onErrorResume(NotificationFailedException.class, e -> {
                            LOG.warn("User created, but notification failed: {}", e.getMessage());
                            return Mono.just(HttpResponse.created(
                                    new UserCreationResponse(convertToUserResponse(savedUser), e.getMessage())));
                        }));
    }

    @Get
    @Operation(summary = "Get all users")
    @Secured("ADMIN")
    public Mono<List<UserResponse>> getAllUsers() {
        return userService.getAllUsers()
                .map(users -> users.stream().map(this::convertToUserResponse).toList());
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
    public Mono<MutableHttpResponse<Map<String, Object>>> deleteUser(@PathVariable UUID id,
            @Header(HttpHeaders.AUTHORIZATION) String authorization) {
        LOG.info("Deleting user with id: {}", id);
        return userService.deleteUser(id, authorization)
                .thenReturn(HttpResponse.ok(Collections.<String, Object>singletonMap("success", true)))
                .onErrorResume(NotificationFailedException.class, e -> {
                    LOG.warn("User deleted, but notification failed: {}", e.getMessage());
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", e.getMessage());
                    return Mono.just(HttpResponse.ok(response));
                });
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

    // Address endpoints (now use userService)
    @Post("/{userId}/addresses")
    @Operation(summary = "Create a new address for a user")
    public Mono<HttpResponse<AddressResponse>> createAddress(@PathVariable UUID userId,
            @Body @Valid CreateAddressRequest request) {
        Address address = convertToAddress(request);
        // Optionally associate address with userId here if needed
        return userService.createAddress(address)
                .map(this::convertToAddressResponse)
                .map(HttpResponse::created);
    }

    @Get("/{userId}/addresses/{id}")
    @Operation(summary = "Get address by ID for a user")
    public Mono<MutableHttpResponse<AddressResponse>> getAddressById(@PathVariable UUID userId, @PathVariable UUID id) {
        return userService.getAddressById(id)
                .map(this::convertToAddressResponse)
                .map(HttpResponse::ok)
                .defaultIfEmpty(HttpResponse.notFound((AddressResponse) null));
    }

    @Put("/{userId}/addresses/{id}")
    @Operation(summary = "Update address by ID for a user")
    public Mono<HttpResponse<AddressResponse>> updateAddress(@PathVariable UUID userId, @PathVariable UUID id,
            @Body @Valid UpdateAddressRequest request) {
        Address address = convertToAddress(request);
        return userService.updateAddress(id, address)
                .map(this::convertToAddressResponse)
                .map(HttpResponse::ok);
    }

    @Delete("/{userId}/addresses/{id}")
    @Operation(summary = "Delete address by ID for a user")
    public Mono<HttpResponse<Void>> deleteAddress(@PathVariable UUID userId, @PathVariable UUID id) {
        return userService.deleteAddress(id)
                .thenReturn(HttpResponse.noContent());
    }

    // Helper methods for address conversion
    private Address convertToAddress(CreateAddressRequest request) {
        Address address = new Address();
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());
        return address;
    }

    private Address convertToAddress(UpdateAddressRequest request) {
        Address address = new Address();
        address.setStreetAddress(request.getStreetAddress());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setCountry(request.getCountry());
        address.setPostalCode(request.getPostalCode());
        address.setAddressType(request.getAddressType());
        return address;
    }

    private AddressResponse convertToAddressResponse(Address address) {
        AddressResponse response = new AddressResponse();
        response.setId(address.getId());
        response.setStreetAddress(address.getStreetAddress());
        response.setCity(address.getCity());
        response.setState(address.getState());
        response.setCountry(address.getCountry());
        response.setPostalCode(address.getPostalCode());
        response.setAddressType(address.getAddressType());
        return response;
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