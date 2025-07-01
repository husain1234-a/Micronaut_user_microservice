package com.yash.usermanagement.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.inject.Singleton;

import com.yash.usermanagement.model.User;

@JdbcRepository(dialect = Dialect.POSTGRES)
@Singleton
public interface UserRepository extends CrudRepository<User, UUID> {
    @Join(value = "address", type = Join.Type.LEFT_FETCH)
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Join(value = "address", type = Join.Type.LEFT_FETCH)
    Optional<User> findById(UUID id);

    @Join(value = "address", type = Join.Type.LEFT_FETCH)
    List<User> findAll();

    Page<User> findAllBy(Pageable pageable);
}