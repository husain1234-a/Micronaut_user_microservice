package com.yash.usermanagement.model;

import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.annotation.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@MappedEntity("addresses")
@Serdeable
public class Address {

    @Id
    @AutoPopulated
    private UUID id;

    @NotBlank(message = "Street address is required")
    @Size(max = 255, message = "Street address must not exceed 255 characters")
    @MappedProperty("st_address")
    private String streetAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @MappedProperty("city")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @MappedProperty("state")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "^[0-9]{5,10}$", message = "Invalid postal code format")
    @MappedProperty("postal_code")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be a 2-letter ISO code")
    @MappedProperty("country")
    private String country;

    @MappedProperty("address_type")
    private AddressType addressType;

    @MappedProperty("address_default")
    private boolean defaultAddress = false;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}