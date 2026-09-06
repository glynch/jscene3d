/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import static io.github.glynch.jscene3d.project.internal.PersistentIds.parse;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable opaque identity of one authored entity entry within an asset.
 *
 * @param value UUID value
 */
public record EntityId(UUID value) {
    /** Validates one entity identity. */
    public EntityId {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Parses a canonical lowercase UUID entity identity.
     *
     * @param value serialized identity
     * @return entity identity
     * @throws IllegalArgumentException if the value is not a canonical lowercase UUID
     */
    public static EntityId from(String value) {
        return new EntityId(parse(value, "value"));
    }

    /** Returns the canonical serialized identity. */
    @Override
    public String toString() {
        return value.toString();
    }
}
