/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.Objects;

/** One component construction deferred until allocation of the complete entity graph. */
record ComponentPlan(
        InternalEntity owner,
        ComponentDefinition definition,
        Map<PropertyId, ProjectValue> overrides,
        String location) {
    /** Copies one validated construction plan. */
    ComponentPlan {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(definition, "definition");
        overrides = Map.copyOf(overrides);
        Objects.requireNonNull(location, "location");
    }
}
