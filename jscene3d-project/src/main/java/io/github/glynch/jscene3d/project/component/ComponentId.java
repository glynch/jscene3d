/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static io.github.glynch.jscene3d.project.internal.PersistentIds.parse;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable opaque identity of one component within its authored entity.
 *
 * @param value UUID value
 */
public record ComponentId(UUID value) {
    /** Validates one component identity. */
    public ComponentId {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Parses a canonical lowercase UUID component identity.
     *
     * @param value serialized identity
     * @return component identity
     * @throws IllegalArgumentException if the value is not a canonical lowercase UUID
     */
    public static ComponentId from(String value) {
        return new ComponentId(parse(value, "value"));
    }

    /** Returns the canonical serialized identity. */
    @Override
    public String toString() {
        return value.toString();
    }
}
