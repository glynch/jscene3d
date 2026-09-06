/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.component;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;

/** Stable local identity of a spatial attachment point.
 *
 * @param value portable lowercase identifier
 */
public record AttachmentPointId(String value) {
    /** Validates the persistent identifier. */
    public AttachmentPointId {
        value = requireLocalId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
