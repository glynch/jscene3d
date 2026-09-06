/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import java.util.List;

/** Live descriptor-backed collision object composed from explicitly referenced shape components. */
public interface CollisionObject3d extends AutoCloseable {
    /**
     * Returns the owning entity.
     *
     * @return entity owning this collision object
     */
    Entity owner();

    /**
     * Returns the authored component identity.
     *
     * @return stable authored component identity
     */
    ComponentId componentId();

    /**
     * Returns member shapes in authored reference order.
     *
     * @return immutable member list
     */
    List<CollisionShape3d> shapes();

    /**
     * Returns whether this object currently participates in physics.
     *
     * @return whether this object currently participates in physics
     */
    boolean isActive();

    /**
     * Returns whether this component has completed terminal cleanup.
     *
     * @return whether this component has completed terminal cleanup
     */
    boolean isClosed();

    /** Releases the backend registration exactly once. */
    @Override
    void close();
}
