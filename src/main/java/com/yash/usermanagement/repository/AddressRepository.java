package com.yash.usermanagement.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.UUID;
import jakarta.inject.Singleton;

import com.yash.usermanagement.model.Address;

@JdbcRepository(dialect = Dialect.POSTGRES)
@Singleton
public interface AddressRepository extends CrudRepository<Address, UUID> {
}