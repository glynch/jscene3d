/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requirePositive;

import java.util.Objects;

/** Exact versioned identity of a registered component type.
 *
 * @param id stable component-type identity
 * @param version positive authored configuration version
 */
public record ComponentType(ComponentTypeId id, int version) {
    /** Validates one exact component type. */
    public ComponentType {
        Objects.requireNonNull(id, "id");
        version = requirePositive(version, "version");
    }

    /**
     * Creates an exact component type from its serialized identity and version.
     *
     * @param id serialized component-type identity
     * @param version positive authored configuration version
     * @return exact component type
     */
    public static ComponentType of(String id, int version) {
        return new ComponentType(new ComponentTypeId(id), version);
    }
}
