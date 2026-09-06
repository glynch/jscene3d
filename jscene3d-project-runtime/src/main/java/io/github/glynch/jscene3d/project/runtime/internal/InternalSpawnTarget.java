/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import io.github.glynch.jscene3d.project.runtime.PreparedEntityDefinition;
import io.github.glynch.jscene3d.project.runtime.SpawnOperation;
import io.github.glynch.jscene3d.project.runtime.SpawnTarget;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.Objects;

/** World-issued spawn capability fixed to one owning entity. */
final class InternalSpawnTarget implements SpawnTarget {
    private final InternalWorld world;
    private final InternalEntity owner;

    /** Stores the validated world and fixed owner. */
    InternalSpawnTarget(InternalWorld world, InternalEntity owner) {
        this.world = Objects.requireNonNull(world, "world");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public Entity owner() {
        return owner;
    }

    @Override
    public SpawnOperation spawn(PreparedEntityDefinition definition, Map<PropertyId, ProjectValue> parameters) {
        return world.spawn(owner, definition, parameters);
    }
}
