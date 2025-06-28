package com.yash.usermanagement.service.impl;

import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yash.usermanagement.exception.DatabaseException;
import com.yash.usermanagement.exception.ResourceNotFoundException;
import com.yash.usermanagement.model.Address;
import com.yash.usermanagement.repository.AddressRepository;
import com.yash.usermanagement.service.AddressService;

import java.util.Optional;
import java.util.UUID;

@Singleton
public class AddressServiceImpl implements AddressService {

    private static final Logger LOG = LoggerFactory.getLogger(AddressServiceImpl.class);
    private final AddressRepository addressRepository;

    public AddressServiceImpl(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional
    public Address createAddress(Address address) {
        try {
            LOG.info("Creating new address");
            Address savedAddress = addressRepository.save(address);
            LOG.info("Address created successfully with ID: {}", savedAddress.getId());
            return savedAddress;
        } catch (Exception e) {
            LOG.error("Error creating address: {}", e.getMessage(), e);
            throw new DatabaseException("Failed to create address", e);
        }
    }

    @Override
    public Optional<Address> getAddressById(UUID id) {
        try {
            LOG.info("Fetching address with ID: {}", id);
            Optional<Address> address = addressRepository.findById(id);
            if (address.isPresent()) {
                LOG.info("Address found with ID: {}", id);
            } else {
                LOG.warn("Address not found with ID: {}", id);
            }
            return address;
        } catch (Exception e) {
            LOG.error("Error fetching address with ID {}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to fetch address", e);
        }
    }

    @Override
    @Transactional
    public Address updateAddress(UUID id, Address address) {
        try {
            LOG.info("Updating address with ID: {}", id);
            return addressRepository.findById(id)
                    .map(existingAddress -> {
                        address.setId(id);
                        Address updatedAddress = addressRepository.update(address);
                        LOG.info("Address updated successfully with ID: {}", id);
                        return updatedAddress;
                    })
                    .orElseThrow(() -> {
                        LOG.warn("Address not found with ID: {}", id);
                        return new ResourceNotFoundException("Address not found with ID: " + id);
                    });
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error updating address with ID {}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to update address", e);
        }
    }

    @Override
    @Transactional
    public void deleteAddress(UUID id) {
        try {
            LOG.info("Deleting address with ID: {}", id);
            if (addressRepository.existsById(id)) {
                addressRepository.deleteById(id);
                LOG.info("Address deleted successfully with ID: {}", id);
            } else {
                LOG.warn("Address not found with ID: {}", id);
                throw new ResourceNotFoundException("Address not found with ID: " + id);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error deleting address with ID {}: {}", id, e.getMessage(), e);
            throw new DatabaseException("Failed to delete address", e);
        }
    }
}