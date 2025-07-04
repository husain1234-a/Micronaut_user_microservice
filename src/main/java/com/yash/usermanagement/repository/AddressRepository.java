package com.yash.usermanagement.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.r2dbc.annotation.R2dbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import java.util.UUID;
import jakarta.inject.Singleton;

import com.yash.usermanagement.model.Address;

@R2dbcRepository(dialect = Dialect.POSTGRES)
@Singleton
public interface AddressRepository extends ReactorCrudRepository<Address, UUID> {
}