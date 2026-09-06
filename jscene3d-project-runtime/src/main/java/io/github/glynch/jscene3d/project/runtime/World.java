/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Runtime ownership and composition root for one authored world.
 *
 * <p>The first composition slice returns an inactive world: its complete graph and component objects exist, but no
 * lifecycle or update callback has run. Closing releases factory-created values which implement {@link AutoCloseable}
 * in reverse construction order. Closing is idempotent.
 */
public interface World extends RuntimeResourceLookup, AutoCloseable {
    /**
     * Returns the validated authored definition used for composition.
     *
     * @return world definition
     */
    WorldDefinition definition();

    /**
     * Returns root entities in deterministic authored order.
     *
     * @return immutable root list
     */
    List<Entity> roots();

    /**
     * Finds a live entity by its world-local identity.
     *
     * @param id live entity identity
     * @return matching entity while present
     */
    Optional<Entity> find(RuntimeEntityId id);

    /**
     * Returns whether this world has released its constructed component values.
     *
     * @return {@code true} after closing
     */
    boolean isClosed();

    /** Releases constructed component values in reverse construction order. */
    @Override
    void close();
}
