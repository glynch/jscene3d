/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import java.util.ArrayList;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** A world-owned body or sensor composed from one or more colliders. */
public abstract sealed class CollisionObject permits CollisionBody, CollisionSensor {
    private static final Vector3fc ZERO = new Vector3f();
    private static final Quaternionfc IDENTITY = new Quaternionf();

    private final PhysicsWorld world;
    private final long id;
    private final Vector3f position;
    private final Quaternionf orientation;
    private final List<Collider> colliders = new ArrayList<>();
    private boolean enabled = true;
    private boolean registered = true;

    CollisionObject(PhysicsWorld world, long id, Vector3fc position, Quaternionfc orientation) {
        this.world = world;
        this.id = id;
        this.position = new Vector3f(position);
        this.orientation = new Quaternionf(orientation);
    }

    /**
     * Returns the stable identifier assigned by the owning world.
     *
     * @return stable world-local identifier
     */
    public final long id() {
        return id;
    }

    /**
     * Adds a collider at the object's origin.
     *
     * @param shape immutable collision shape
     * @return object-owned collider
     */
    public final Collider addCollider(CollisionShape shape) {
        return addCollider(shape, ZERO, IDENTITY);
    }

    /**
     * Adds a collider with a transform relative to this object.
     *
     * @param shape immutable collision shape
     * @param localPosition collider position relative to this object
     * @param localOrientation collider orientation relative to this object
     * @return object-owned collider
     */
    public final Collider addCollider(CollisionShape shape, Vector3fc localPosition, Quaternionfc localOrientation) {
        requireRegistered();
        return world.addCollider(this, shape, localPosition, localOrientation);
    }

    /**
     * Removes an owned collider.
     *
     * @param collider collider to remove
     */
    public final void removeCollider(Collider collider) {
        requireRegistered();
        world.removeCollider(this, collider);
    }

    /**
     * Returns the colliders owned by this object in insertion order.
     *
     * @return immutable collider list
     */
    public final List<Collider> colliders() {
        return List.copyOf(colliders);
    }

    /**
     * Copies the world-space position into the destination.
     *
     * @param destination vector to receive the position
     * @return the supplied destination
     */
    public final Vector3f position(Vector3f destination) {
        return destination.set(position);
    }

    /**
     * Copies the world-space orientation into the destination.
     *
     * @param destination quaternion to receive the orientation
     * @return the supplied destination
     */
    public final Quaternionf orientation(Quaternionf destination) {
        return destination.set(orientation);
    }

    /**
     * Returns whether this object and its colliders participate in queries.
     *
     * @return {@code true} when enabled
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables this object and all its colliders.
     *
     * @param newEnabled whether this object participates in queries
     */
    public final void setEnabled(boolean newEnabled) {
        requireRegistered();
        enabled = newEnabled;
    }

    /**
     * Returns whether this handle remains registered with its world.
     *
     * @return {@code true} while registered
     */
    public final boolean isRegistered() {
        return registered;
    }

    final PhysicsWorld world() {
        return world;
    }

    final void attach(Collider collider) {
        colliders.add(collider);
    }

    final void detach(Collider collider) {
        colliders.remove(collider);
    }

    final void applyTransform(Vector3fc newPosition, Quaternionfc newOrientation) {
        position.set(newPosition);
        orientation.set(newOrientation);
    }

    final void markRemoved() {
        registered = false;
        colliders.forEach(Collider::markRemoved);
        colliders.clear();
    }

    final void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException("collision object is no longer registered with its world");
        }
    }
}
