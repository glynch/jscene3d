/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;

/** Capability which may spawn direct children beneath one fixed owning entity. */
public interface SpawnTarget {
    /**
     * Returns the entity which will own each successfully spawned root.
     *
     * @return fixed owner
     */
    Entity owner();

    /**
     * Requests one instance of a prepared definition using exported ordinary parameters.
     *
     * <p>An idle world commits synchronously before this method returns. A request made during an update or signal
     * callback remains pending until the next structural boundary. The instance cannot participate in the phase which
     * requested it, but after publication it may participate in later phases of the same fixed step.
     *
     * @param definition world-bound prepared definition
     * @param parameters exported non-resource parameter values for this instance
     * @return observable spawn operation
     * @throws IllegalArgumentException if the preparation belongs to another world
     * @throws IllegalStateException if the world is inactive or closed, or the owner is pending or destroyed
     */
    SpawnOperation spawn(PreparedEntityDefinition definition, Map<PropertyId, ProjectValue> parameters);

    /**
     * Requests one instance without exported ordinary parameters.
     *
     * @param definition world-bound prepared definition
     * @return observable spawn operation
     */
    default SpawnOperation spawn(PreparedEntityDefinition definition) {
        return spawn(definition, Map.of());
    }
}
