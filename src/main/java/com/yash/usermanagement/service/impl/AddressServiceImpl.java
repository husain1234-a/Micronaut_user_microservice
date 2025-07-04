package com.yash.usermanagement.service.impl;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yash.usermanagement.exception.DatabaseException;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.model.Address;
import com.yash.usermanagement.repository.AddressRepository;
import com.yash.usermanagement.service.AddressService;

import reactor.core.publisher.Mono;
import java.util.UUID;

@Singleton
public class AddressServiceImpl implements AddressService {

    private static final Logger LOG = LoggerFactory.getLogger(AddressServiceImpl.class);
    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public Mono<Address> createAddress(Address address) {
        LOG.info("Creating new address");
        return addressRepository.save(address)
            .doOnSuccess(savedAddress -> LOG.info("Address created successfully with ID: {}", savedAddress.getId()))
            .onErrorMap(e -> {
                LOG.error("Error creating address: {}", e.getMessage(), e);
                return new DatabaseException("Failed to create address", e);
            });
    }

    @Override
    public Mono<Address> getAddressById(UUID id) {
        LOG.info("Fetching address with ID: {}", id);
        return addressRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Address not found with ID: " + id)))
            .doOnSuccess(address -> LOG.info("Address found with ID: {}", id))
            .onErrorMap(e -> {
                LOG.error("Error fetching address with ID {}: {}", id, e.getMessage(), e);
                return new DatabaseException("Failed to fetch address", e);
            });
    }

    @Override
    public Mono<Address> updateAddress(UUID id, Address address) {
        LOG.info("Updating address with ID: {}", id);
        return addressRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Address not found with ID: " + id)))
            .flatMap(existingAddress -> {
                address.setId(id);
                return addressRepository.update(address)
                    .doOnSuccess(updatedAddress -> LOG.info("Address updated successfully with ID: {}", id));
            })
            .onErrorMap(e -> {
                LOG.error("Error updating address with ID {}: {}", id, e.getMessage(), e);
                return new DatabaseException("Failed to update address", e);
            });
    }

    @Override
    public Mono<Void> deleteAddress(UUID id) {
        LOG.info("Deleting address with ID: {}", id);
        return addressRepository.existsById(id)
            .flatMap(exists -> {
                if (exists) {
                    return addressRepository.deleteById(id)
                        .doOnSuccess(v -> LOG.info("Address deleted successfully with ID: {}", id))
                        .then();
                } else {
                    LOG.warn("Address not found with ID: {}", id);
                    return Mono.error(new ResourceNotFoundException("Address not found with ID: " + id));
                }
            })
            .onErrorMap(e -> {
                LOG.error("Error deleting address with ID {}: {}", id, e.getMessage(), e);
                return new DatabaseException("Failed to delete address", e);
            });
    }
}