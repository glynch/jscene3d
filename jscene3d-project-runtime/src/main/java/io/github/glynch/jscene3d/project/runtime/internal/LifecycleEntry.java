/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeObject;
import java.util.Objects;

/** Couples one owned runtime object to its effective enabled state. */
record LifecycleEntry(ProjectRuntimeObject object, boolean enabled) {
    /** Validates the lifecycle entry. */
    LifecycleEntry {
        Objects.requireNonNull(object, "object");
    }
}
