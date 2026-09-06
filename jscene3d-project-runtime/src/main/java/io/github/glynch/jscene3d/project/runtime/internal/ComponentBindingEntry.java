/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import java.util.Objects;

/** One constructed component awaiting instance-scoped authored reference binding. */
record ComponentBindingEntry(
        ComponentPlan plan, ComponentReferenceBinder binder, EffectiveComponentProperties properties) {
    /** Validates one pending binding entry. */
    ComponentBindingEntry {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(binder, "binder");
        Objects.requireNonNull(properties, "properties");
    }
}
