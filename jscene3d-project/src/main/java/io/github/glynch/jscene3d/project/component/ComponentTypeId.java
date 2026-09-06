/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireRegisteredTypeId;

/**
 * Stable namespaced identity of one registered component type.
 *
 * @param value extension-qualified component type identity
 */
public record ComponentTypeId(String value) {
    /** Validates one component type identity. */
    public ComponentTypeId {
        value = requireRegisteredTypeId(value, "value");
    }

    /** Returns the serialized component type identity. */
    @Override
    public String toString() {
        return value;
    }
}
