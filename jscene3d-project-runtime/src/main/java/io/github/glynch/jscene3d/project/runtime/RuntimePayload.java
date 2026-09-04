/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.extension.RegisteredType;
import java.util.Objects;

/** Typed value carried by a runtime signal.
 *
 * @param type registered payload type
 * @param value extension-defined immutable or safely shared value
 */
public record RuntimePayload(RegisteredType type, Object value) {
    /** Validates the payload. */
    public RuntimePayload {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
    }
}
