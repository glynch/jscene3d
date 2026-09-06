/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.Map;
import java.util.Objects;

/** One authored component whose reusable resources must be prepared before spawning. */
record ComponentPreparationPlan(
        EntityInstanceScope scope,
        EntityId authoredEntity,
        ComponentDefinition definition,
        Map<PropertyId, ScopedProjectValue> overrides,
        String location) {
    /** Copies one validated preparation plan. */
    ComponentPreparationPlan {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(authoredEntity, "authoredEntity");
        Objects.requireNonNull(definition, "definition");
        overrides = Map.copyOf(overrides);
        Objects.requireNonNull(location, "location");
    }
}
