/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.composition.CompositionPlan;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.runtime.PreparedEntityDefinition;
import io.github.glynch.jscene3d.project.runtime.World;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** Opaque reusable-definition graph and fixed resource bindings prepared for one world. */
public final class InternalPreparedEntityDefinition implements PreparedEntityDefinition {
    private final InternalWorld world;
    private final AssetRef<EntityDefinition> reference;
    private final CompositionPlan plan;
    private final Map<PropertyId, ProjectValue> resourceBindings;
    private final URI source;

    /** Stores one successfully prepared transitive graph. */
    InternalPreparedEntityDefinition(
            InternalWorld world,
            AssetRef<EntityDefinition> reference,
            CompositionPlan plan,
            Map<PropertyId, ProjectValue> resourceBindings,
            URI source) {
        this.world = Objects.requireNonNull(world, "world");
        this.reference = Objects.requireNonNull(reference, "reference");
        this.plan = Objects.requireNonNull(plan, "plan");
        this.resourceBindings = Map.copyOf(resourceBindings);
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public AssetRef<EntityDefinition> reference() {
        return reference;
    }

    @Override
    public World world() {
        return world;
    }

    /** Returns the immutable prepared definition graph and effective fixed bindings. */
    CompositionPlan plan() {
        return plan;
    }

    /** Returns fixed public resource bindings supplied during preparation. */
    Map<PropertyId, ProjectValue> resourceBindings() {
        return resourceBindings;
    }

    /** Returns the root definition source for runtime diagnostics. */
    URI source() {
        return source;
    }
}
