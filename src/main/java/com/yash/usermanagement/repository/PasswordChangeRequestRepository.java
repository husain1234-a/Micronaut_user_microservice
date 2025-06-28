package com.yash.usermanagement.repository;

import com.yash.usermanagement.model.PasswordChangeRequest;
import com.yash.usermanagement.model.PasswordChangeStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.inject.Singleton;

@JdbcRepository(dialect = Dialect.POSTGRES)
@Singleton
public interface PasswordChangeRequestRepository extends CrudRepository<PasswordChangeRequest, UUID> {
    List<PasswordChangeRequest> findByUserId(UUID userId);

    List<PasswordChangeRequest> findByStatus(PasswordChangeStatus status);

    Optional<PasswordChangeRequest> findByUserIdAndStatus(UUID userId, PasswordChangeStatus status);
}