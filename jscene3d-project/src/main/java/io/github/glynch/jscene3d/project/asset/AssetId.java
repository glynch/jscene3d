/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.asset;

import static io.github.glynch.jscene3d.project.internal.PersistentIds.parse;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable opaque identity of one independently stored project asset.
 *
 * <p>An asset ID is independent of the asset's display name and filesystem location.
 *
 * @param value UUID value
 */
public record AssetId(UUID value) {
    /** Validates one asset identity. */
    public AssetId {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Parses a canonical lowercase UUID asset identity.
     *
     * @param value serialized identity
     * @return asset identity
     * @throws IllegalArgumentException if the value is not a canonical lowercase UUID
     */
    public static AssetId from(String value) {
        return new AssetId(parse(value, "value"));
    }

    /** Returns the canonical serialized identity. */
    @Override
    public String toString() {
        return value.toString();
    }
}
