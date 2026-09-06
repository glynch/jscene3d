/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.internal;

import java.util.Objects;
import java.util.UUID;

/** Shared parsing policy for opaque persistent UUID identities. */
public final class PersistentIds {
    /** Prevents construction of this policy container. */
    private PersistentIds() {
        throw new AssertionError("PersistentIds cannot be instantiated");
    }

    /**
     * Parses one canonical lowercase UUID.
     *
     * @param value serialized UUID
     * @param name argument name used in failures
     * @return parsed UUID
     */
    public static UUID parse(String value, String name) {
        String text = Objects.requireNonNull(value, name);
        UUID parsed;
        try {
            parsed = UUID.fromString(text);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a canonical lowercase UUID: " + value, exception);
        }
        if (!parsed.toString().equals(text)) {
            throw new IllegalArgumentException(name + " must be a canonical lowercase UUID: " + value);
        }
        return parsed;
    }
}
