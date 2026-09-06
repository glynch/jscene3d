/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.runtime.Entity;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * One independently transformed collision-shape component available for explicit body membership.
 *
 * <p>The shape resource is retained by the owning world. This component does not infer membership from entity
 * hierarchy or rendered geometry. The first collision profile requires a shape and its collision object to be sibling
 * components on the same entity.
 */
public final class CollisionShape3d implements AutoCloseable {
    private final Entity owner;
    private final ComponentId componentId;
    private final CollisionShape3dResource resource;
    private final Vector3f localPosition;
    private final Quaternionf localOrientation;
    private final CollisionFilter3d filter;
    private boolean closed;

    /** Stores one validated descriptor-backed shape component. */
    CollisionShape3d(
            Entity owner,
            ComponentId componentId,
            CollisionShape3dResource resource,
            Vector3fc localPosition,
            Quaternionfc localOrientation,
            CollisionFilter3d filter) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.componentId = Objects.requireNonNull(componentId, "componentId");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.localPosition = CollisionPreconditions.requireFinite(localPosition, "localPosition");
        this.localOrientation = CollisionPreconditions.requireOrientation(localOrientation, "localOrientation");
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    /**
     * Returns the owning entity.
     *
     * @return live entity owning this authored shape record
     */
    public Entity owner() {
        return owner;
    }

    /**
     * Returns the authored component identity.
     *
     * @return stable authored component identity
     */
    public ComponentId componentId() {
        return componentId;
    }

    /**
     * Returns the retained immutable-use geometry.
     *
     * @return shape resource
     * @throws IllegalStateException if this component is closed
     */
    public CollisionShape3dResource resource() {
        requireOpen();
        return resource;
    }

    /**
     * Returns an immutable snapshot of the position relative to the collision object.
     *
     * @return local position
     * @throws IllegalStateException if this component is closed
     */
    public Vector3fc localPosition() {
        requireOpen();
        return new Vector3f(localPosition);
    }

    /**
     * Returns an immutable snapshot of the orientation relative to the collision object.
     *
     * @return normalized local orientation
     * @throws IllegalStateException if this component is closed
     */
    public Quaternionfc localOrientation() {
        requireOpen();
        return new Quaternionf(localOrientation);
    }

    /**
     * Returns the authored interaction filter.
     *
     * @return collision filter
     * @throws IllegalStateException if this component is closed
     */
    public CollisionFilter3d filter() {
        requireOpen();
        return filter;
    }

    /**
     * Returns whether world-owned terminal cleanup has completed.
     *
     * @return {@code true} after world-owned terminal cleanup
     */
    public boolean isClosed() {
        return closed;
    }

    /** Marks this component terminal without closing its separately leased resource. */
    @Override
    public void close() {
        closed = true;
    }

    /** Rejects component state access after cleanup. */
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("CollisionShape3d is closed");
        }
    }
}
