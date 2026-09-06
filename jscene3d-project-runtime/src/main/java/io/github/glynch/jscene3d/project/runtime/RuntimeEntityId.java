/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/**
 * Opaque identity of one live entity within its owning {@link World}.
 *
 * <p>The value is unrelated to display names, hierarchy position, authored asset paths, or persistent entity IDs.
 * Values are unique for the lifetime of one world and are not persistent across compositions.
 *
 * @param value positive world-local identity
 */
public record RuntimeEntityId(long value) {
    /** Validates one live entity identity. */
    public RuntimeEntityId {
        if (value < 1L) {
            throw new IllegalArgumentException("value must be positive");
        }
    }
}
