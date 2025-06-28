package com.yash.usermanagement.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;

import com.yash.usermanagement.dto.AddressResponse;
import com.yash.usermanagement.dto.CreateAddressRequest;
import com.yash.usermanagement.dto.UpdateAddressRequest;
import com.yash.usermanagement.model.Address;
import com.yash.usermanagement.service.AddressService;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.exception.ValidationException;

@Controller("/api/addresses")
@Tag(name = "Address Management")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Post
    @Operation(summary = "Create a new address")
    public HttpResponse<AddressResponse> createAddress(@Body @Valid CreateAddressRequest request) {
        try {
            Address address = convertToAddress(request);
            Address createdAddress = addressService.createAddress(address);
            return HttpResponse.created(convertToAddressResponse(createdAddress));
        } catch (ValidationException e) {
            throw e;
        }
    }

    @Get("/{id}")
    @Operation(summary = "Get address by ID")
    public HttpResponse<AddressResponse> getAddressById(@PathVariable UUID id) {
        try {
            return addressService.getAddressById(id)
                    .map(this::convertToAddressResponse)
                    .map(HttpResponse::ok)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + id));
        } catch (ResourceNotFoundException e) {
            throw e;
        }
    }

    @Put("/{id}")
    @Operation(summary = "Update address by ID")
    public HttpResponse<AddressResponse> updateAddress(@PathVariable UUID id,
            @Body @Valid UpdateAddressRequest request) {
        try {
            Address address = convertToAddress(request);
            Address updatedAddress = addressService.updateAddress(id, address);
            return HttpResponse.ok(convertToAddressResponse(updatedAddress));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (ValidationException e) {
            throw e;
        }
    }

    @Delete("/{id}")
    @Operation(summary = "Delete address by ID")
    public HttpResponse<Void> deleteAddress(@PathVariable UUID id) {
        try {
            addressService.deleteAddress(id);
            return HttpResponse.noContent();
        } catch (ResourceNotFoundException e) {
            throw e;
        }
    }

    // Helper methods for conversion
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
}