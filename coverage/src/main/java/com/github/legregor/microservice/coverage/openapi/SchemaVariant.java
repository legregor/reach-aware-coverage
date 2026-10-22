package com.github.legregor.microservice.coverage.openapi;

import java.util.Set;

/**
 * One alternative of a {@code oneOf} / {@code anyOf} schema.
 *
 * @param name               discriminator value, else the name of the referenced schema, else {@code option<i>}
 * @param requiredProperties properties the alternative requires (used to recognise it in a value)
 */
public record SchemaVariant(String name, Set<String> requiredProperties) {

    public SchemaVariant {
        requiredProperties = requiredProperties == null ? Set.of() : Set.copyOf(requiredProperties);
    }
}
