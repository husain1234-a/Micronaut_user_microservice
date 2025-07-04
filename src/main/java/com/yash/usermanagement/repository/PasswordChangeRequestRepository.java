package com.yash.usermanagement.repository;

import com.yash.usermanagement.model.PasswordChangeRequest;
import com.yash.usermanagement.model.PasswordChangeStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.r2dbc.annotation.R2dbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
import jakarta.inject.Singleton;

@R2dbcRepository(dialect = Dialect.POSTGRES)
@Singleton
public interface PasswordChangeRequestRepository extends ReactorCrudRepository<PasswordChangeRequest, UUID> {
    Flux<PasswordChangeRequest> findByUserId(UUID userId);

    Flux<PasswordChangeRequest> findByStatus(PasswordChangeStatus status);

    Mono<PasswordChangeRequest> findByUserIdAndStatus(UUID userId, PasswordChangeStatus status);
}