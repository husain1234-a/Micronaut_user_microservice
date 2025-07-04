package com.yash.usermanagement.repository;

import com.yash.usermanagement.model.UserDevice;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.r2dbc.annotation.R2dbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

@R2dbcRepository(dialect = Dialect.POSTGRES)
public interface UserDeviceRepository extends ReactorCrudRepository<UserDevice, UUID> {
    Flux<UserDevice> findByUserId(UUID userId);
} 