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
 * <p>Composition returns an inactive world: its complete graph and component objects exist, but no lifecycle callback
 * has run. {@link #activate()} transactionally creates and activates declared component lifecycles. This interface is
 * not thread-safe; composition, activation, queries, and closure belong to one caller-owned logical simulation thread.
 * Closing releases lifecycle participation and factory-created values in reverse ownership and construction order.
 * Closing is idempotent and terminal.
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
     * Transactionally enters semantic lifecycle management and activates every initially enabled entity.
     *
     * <p>Creation and activation run owner before owned descendants. A callback failure compensates completed work in
     * reverse order, closes every component value, and leaves this world closed. A world may be activated only once.
     *
     * @throws IllegalStateException if composition is incomplete, activation is already underway, this world is
     *     already active, or this world is closed
     * @throws WorldLifecycleException if a declared component callback fails
     */
    void activate();

    /**
     * Returns whether activation completed successfully and closure has not begun.
     *
     * @return {@code true} only for an active world
     */
    boolean isActive();

    /**
     * Returns whether this world has released its constructed component values.
     *
     * @return {@code true} after closing
     */
    boolean isClosed();

    /**
     * Deactivates and destroys lifecycle participants, then releases component values in reverse order.
     *
     * @throws WorldLifecycleException if a declared cleanup callback fails after all cleanup has been attempted
     */
    @Override
    void close();
}
