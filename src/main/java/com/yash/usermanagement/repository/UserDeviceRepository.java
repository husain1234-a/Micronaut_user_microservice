package com.yash.usermanagement.repository;

import com.yash.usermanagement.model.UserDevice;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface UserDeviceRepository extends CrudRepository<UserDevice, UUID> {
    List<UserDevice> findByUserId(UUID userId);
} 