/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Objects;

/** One effective property value plus the instance scope in which its authored targets were declared. */
record ScopedProjectValue(ProjectValue value, EntityInstanceScope targetScope) {
    /** Validates one scoped value. */
    ScopedProjectValue {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(targetScope, "targetScope");
    }
}
