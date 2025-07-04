package com.yash.usermanagement.service;

import com.yash.usermanagement.model.Address;
import reactor.core.publisher.Mono;
import java.util.UUID;

public interface AddressService {
    Mono<Address> createAddress(Address address);

    Mono<Address> getAddressById(UUID id);

    Mono<Address> updateAddress(UUID id, Address address);

    Mono<Void> deleteAddress(UUID id);
}