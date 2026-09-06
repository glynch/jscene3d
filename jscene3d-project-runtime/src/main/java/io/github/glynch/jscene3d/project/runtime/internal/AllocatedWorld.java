/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import java.util.List;
import java.util.Objects;

/** Complete entity allocation plus component work awaiting transactional construction. */
record AllocatedWorld(InternalWorld world, List<ComponentPlan> components) {
    /** Copies one allocation result. */
    AllocatedWorld {
        Objects.requireNonNull(world, "world");
        components = List.copyOf(components);
    }
}
