/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.util.Objects;

/** Versioned type supplied by the engine or a project extension.
 *
 * @param id extension-qualified type identifier
 * @param version positive definition version
 */
public record RegisteredType(String id, int version) {
    /** Validates one registered type reference. */
    public RegisteredType {
        if (Objects.requireNonNull(id, "id").isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be positive");
        }
    }
}
