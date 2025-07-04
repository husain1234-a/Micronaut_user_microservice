package com.yash.usermanagement.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.r2dbc.annotation.R2dbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.reactive.ReactorCrudRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import java.util.UUID;
import jakarta.inject.Singleton;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import com.yash.usermanagement.model.User;

@R2dbcRepository(dialect = Dialect.POSTGRES)
@Singleton
public interface UserRepository extends ReactorCrudRepository<User, UUID> {
    @Join(value = "address", type = Join.Type.LEFT_FETCH)
    Mono<User> findByEmail(String email);

    Mono<Boolean> existsByEmail(String email);

    @Join(value = "address", type = Join.Type.LEFT_FETCH)
    Mono<User> findById(UUID id);

    @Join(value = "address", type = Join.Type.LEFT_FETCH)
    Flux<User> findAll();

    Mono<Page<User>> findAll(Pageable pageable);
}