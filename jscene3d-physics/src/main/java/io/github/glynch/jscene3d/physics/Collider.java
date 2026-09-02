/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** A world-owned collision object and its transform and filtering state. */
public final class Collider {
    private final PhysicsWorld world;
    private final long id;
    private final CollisionShape shape;
    private final Vector3f position;
    private final Quaternionf orientation;
    private CollisionFilter collisionFilter = CollisionFilter.DEFAULT;
    private boolean trigger;
    private boolean enabled = true;
    private boolean registered = true;

    Collider(PhysicsWorld world, long id, CollisionShape shape, Vector3fc position, Quaternionfc orientation) {
        this.world = world;
        this.id = id;
        this.shape = Objects.requireNonNull(shape, "shape");
        this.position = new Vector3f(position);
        this.orientation = new Quaternionf(orientation);
    }

    /**
     * Returns the stable identifier assigned by the owning world.
     *
     * @return stable world-local identifier
     */
    public long id() {
        return id;
    }

    /**
     * Returns the immutable shape.
     *
     * @return collision shape
     */
    public CollisionShape shape() {
        return shape;
    }

    /**
     * Copies the world-space position into the destination.
     *
     * @param destination vector to receive the position
     * @return the supplied destination
     */
    public Vector3f position(Vector3f destination) {
        return destination.set(position);
    }

    /**
     * Copies the world-space orientation into the destination.
     *
     * @param destination quaternion to receive the orientation
     * @return the supplied destination
     */
    public Quaternionf orientation(Quaternionf destination) {
        return destination.set(orientation);
    }

    /**
     * Replaces the world-space transform and updates the spatial index.
     *
     * @param newPosition new world-space position
     * @param newOrientation new world-space orientation; normalized internally
     */
    public void setTransform(Vector3fc newPosition, Quaternionfc newOrientation) {
        requireRegistered();
        world.updateTransform(this, newPosition, newOrientation);
    }

    /**
     * Returns the collision category and mask.
     *
     * @return collision filter
     */
    public CollisionFilter collisionFilter() {
        return collisionFilter;
    }

    /**
     * Replaces the collision category and mask.
     *
     * @param newCollisionFilter replacement filter
     */
    public void setCollisionFilter(CollisionFilter newCollisionFilter) {
        requireRegistered();
        collisionFilter = Objects.requireNonNull(newCollisionFilter, "collisionFilter");
    }

    /**
     * Returns whether this collider is a non-solid trigger.
     *
     * @return {@code true} for a trigger collider
     */
    public boolean isTrigger() {
        return trigger;
    }

    /**
     * Sets whether this collider is a non-solid trigger.
     *
     * @param newTrigger whether the collider is a trigger
     */
    public void setTrigger(boolean newTrigger) {
        requireRegistered();
        trigger = newTrigger;
    }

    /**
     * Returns whether queries can hit this collider.
     *
     * @return {@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables this collider for queries.
     *
     * @param newEnabled whether queries can hit this collider
     */
    public void setEnabled(boolean newEnabled) {
        requireRegistered();
        enabled = newEnabled;
    }

    /**
     * Returns whether this handle remains registered with its world.
     *
     * @return {@code true} while registered
     */
    public boolean isRegistered() {
        return registered;
    }

    void applyTransform(Vector3fc newPosition, Quaternionfc newOrientation) {
        position.set(newPosition);
        orientation.set(newOrientation);
    }

    void markRemoved() {
        registered = false;
    }

    PhysicsWorld world() {
        return world;
    }

    private void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException("collider is no longer registered with its world");
        }
    }
}
