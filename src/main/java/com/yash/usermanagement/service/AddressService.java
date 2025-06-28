package com.yash.usermanagement.service;

import java.util.Optional;
import java.util.UUID;

import com.yash.usermanagement.model.Address;

public interface AddressService {
    Address createAddress(Address address);

    Optional<Address> getAddressById(UUID id);

    Address updateAddress(UUID id, Address address);

    void deleteAddress(UUID id);
}