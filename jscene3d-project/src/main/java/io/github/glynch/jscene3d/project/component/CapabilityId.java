/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireRegisteredTypeId;

/** Stable extension-qualified identity of a component or definition capability.
 *
 * @param value extension-qualified identifier
 */
public record CapabilityId(String value) {
    /** Validates the persistent identifier. */
    public CapabilityId {
        value = requireRegisteredTypeId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
