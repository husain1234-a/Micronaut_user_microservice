package com.yash.usermanagement.config;

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Singleton;

import java.util.Optional;
import java.util.UUID;

@Factory
public class TypeConverterConfig {

    @Singleton
    TypeConverter<String, UUID> stringToUUIDConverter() {
        return (object, targetType, context) -> {
            try {
                return Optional.of(UUID.fromString(object));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        };
    }

    @Singleton
    TypeConverter<UUID, String> uuidToStringConverter() {
        return (object, targetType, context) -> Optional.of(object.toString());
    }
}