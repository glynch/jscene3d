/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import java.util.List;
import java.util.Objects;

/** Detached entity allocation plus deferred component and connection work for one spawn transaction. */
record AllocatedInstance(
        InternalWorld world,
        InternalEntity root,
        List<InternalEntity> entities,
        List<ComponentPlan> components,
        List<RuntimeConnectionPlan> connections) {
    /** Copies one complete detached allocation. */
    AllocatedInstance {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(root, "root");
        entities = List.copyOf(entities);
        components = List.copyOf(components);
        connections = List.copyOf(connections);
    }
}
