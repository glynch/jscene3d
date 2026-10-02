/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.composition.CompositionComponent;
import io.github.glynch.jscene3d.project.composition.CompositionValue;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.util.Map;
import java.util.Objects;

/** One component construction deferred until allocation of the complete entity graph. */
record ComponentPlan(InternalEntity owner, CompositionComponent composition, RuntimeCompositionScopes scopes) {
    /** Copies one validated construction plan. */
    ComponentPlan {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(composition, "composition");
        Objects.requireNonNull(scopes, "scopes");
    }

    /** Returns the original authored component definition. */
    ComponentDefinition definition() {
        return composition.definition();
    }

    /** Returns the exact inert descriptor selected during safe planning. */
    ComponentTypeDescriptor descriptor() {
        return composition.descriptor();
    }

    /** Returns the authored local entity identity. */
    EntityId authoredEntity() {
        return composition.authoredEntity();
    }

    /** Returns descriptor-ordered effective values with their authored scopes. */
    Map<PropertyId, CompositionValue> properties() {
        return composition.effectiveProperties();
    }

    /** Returns the stable diagnostic location. */
    String location() {
        return composition.location();
    }
}
