/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Runtime ownership and composition root for one authored world.
 *
 * <p>Composition returns an inactive world: its complete graph and component objects exist, but no lifecycle callback
 * has run. {@link #activate()} transactionally creates and activates declared component lifecycles. This interface is
 * not thread-safe; composition, activation, queries, and closure belong to one caller-owned logical simulation thread.
 * Endpoint dispatch begins only after successful activation and stops before closure releases lifecycle participation
 * and factory-created values in reverse ownership and construction order. Closing is idempotent and terminal.
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
     * Advances one fixed simulation step through the before-physics and after-physics component phases.
     *
     * <p>The world owns the zero-based tick and accumulated simulation time supplied to callbacks. Components
     * participate only when their exact descriptor declares the corresponding phase and their owning entity is
     * enabled. This method may be called only on an active world and is not reentrant.
     *
     * @param step positive fixed-step duration
     * @throws IllegalArgumentException if {@code step} is not positive
     * @throws IllegalStateException if the world is not active or another update is executing
     * @throws WorldUpdateException if a scheduled component callback fails
     */
    void advanceFixed(Duration step);

    /**
     * Advances presentation behavior once for a rendered frame.
     *
     * <p>The current completed simulation time and supplied interpolation fraction are exposed through an immutable
     * callback context. This method may be called only on an active world and is not reentrant.
     *
     * @param elapsed non-negative real time accepted for the frame
     * @param interpolation finite fixed-step interpolation fraction in the inclusive unit interval
     * @throws IllegalArgumentException if {@code elapsed} is negative or {@code interpolation} is invalid
     * @throws IllegalStateException if the world is not active or another update is executing
     * @throws WorldUpdateException if a scheduled component callback fails
     */
    void advanceFrame(Duration elapsed, float interpolation);

    /**
     * Requests that one live entity become locally enabled.
     *
     * <p>An idle active world applies the request immediately. A request made by an update callback commits after the
     * current component-visible phase. Effective descendant activation is delivered owner first. Repeating an
     * already-effective request is a no-op.
     *
     * @param entity entity owned by this world
     * @throws IllegalArgumentException if {@code entity} belongs to another world
     * @throws IllegalStateException if the world is not active, structural mutation is already committing, or the
     *     entity is pending or completely destroyed
     * @throws WorldLifecycleException if a lifecycle callback fails and closes the world
     */
    void enable(Entity entity);

    /**
     * Requests that one live entity become locally disabled.
     *
     * <p>An idle active world applies the request immediately. A request made by an update callback commits after the
     * current component-visible phase. Effective descendant deactivation is delivered child first. Repeating an
     * already-effective request is a no-op.
     *
     * @param entity entity owned by this world
     * @throws IllegalArgumentException if {@code entity} belongs to another world
     * @throws IllegalStateException if the world is not active, structural mutation is already committing, or the
     *     entity is pending or completely destroyed
     * @throws WorldLifecycleException if a lifecycle callback fails and closes the world
     */
    void disable(Entity entity);

    /**
     * Requests permanent destruction of one live entity and its owned subtree.
     *
     * <p>The subtree stops participating immediately. An idle active world commits destruction immediately; a request
     * made by an update callback commits after the current component-visible phase. The commit deactivates and
     * destroys child first, closes every owned component value, and removes the subtree from world lookup and the live
     * hierarchy. Repeated destruction requests are no-ops.
     *
     * @param entity entity owned by this world
     * @throws IllegalArgumentException if {@code entity} belongs to another world
     * @throws IllegalStateException if the world is not active or structural mutation is already committing
     * @throws WorldLifecycleException if a lifecycle callback fails and closes the world after cleanup
     */
    void destroy(Entity entity);

    /**
     * Deactivates and destroys lifecycle participants, then releases component values in reverse order.
     *
     * @throws WorldLifecycleException if a declared cleanup callback fails after all cleanup has been attempted
     */
    @Override
    void close();
}
