/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import io.github.glynch.jscene3d.physics.internal.CollisionQueries;
import io.github.glynch.jscene3d.physics.internal.Preconditions;
import io.github.glynch.jscene3d.physics.internal.ShapePose;
import io.github.glynch.jscene3d.physics.internal.WorldIndex;
import io.github.glynch.jscene3d.physics.queries.OverlapHit;
import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.RaycastHit;
import io.github.glynch.jscene3d.physics.queries.SweepHit;
import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Owns colliders and answers deterministic three-dimensional collision queries. */
public final class PhysicsWorld {
    private static final Vector3fc ZERO = new Vector3f();
    private static final Quaternionfc IDENTITY = new Quaternionf();
    private static final float MINIMUM_DIRECTION_LENGTH_SQUARED = 1.0E-12F;

    private final Set<Collider> colliders;
    private final WorldIndex index;
    private final CollisionQueries queries;
    private long nextColliderId = 1L;

    /** Creates an empty physics world. */
    public PhysicsWorld() {
        colliders = new LinkedHashSet<>();
        index = new WorldIndex();
        queries = new CollisionQueries(index);
    }

    /**
     * Adds a collider at the world origin with identity orientation.
     *
     * @param shape immutable collision shape
     * @return world-owned collider handle
     */
    public Collider addCollider(CollisionShape shape) {
        return addCollider(shape, ZERO, IDENTITY);
    }

    /**
     * Adds a collider and copies the supplied world transform.
     *
     * @param shape immutable collision shape
     * @param position world-space position
     * @param orientation world-space orientation; normalized internally
     * @return world-owned collider handle
     */
    public Collider addCollider(CollisionShape shape, Vector3fc position, Quaternionfc orientation) {
        ShapePose pose = new ShapePose(shape, position, orientation);
        Collider collider = new Collider(
                this,
                nextColliderId++,
                pose.shape(),
                pose.position(new Vector3f()),
                pose.orientation(new Quaternionf()));
        colliders.add(collider);
        index.add(collider, pose);
        return collider;
    }

    /**
     * Removes a collider. Its readable state remains available, but it can no longer be mutated.
     *
     * @param collider registered collider owned by this world
     */
    public void remove(Collider collider) {
        requireOwnedAndRegistered(collider);
        index.remove(collider);
        colliders.remove(collider);
        collider.markRemoved();
    }

    /** Removes every collider and invalidates every outstanding handle. */
    public void clear() {
        for (Collider collider : colliders) {
            collider.markRemoved();
        }
        colliders.clear();
        index.clear();
    }

    /**
     * Returns the number of registered colliders.
     *
     * @return registered collider count
     */
    public int colliderCount() {
        return colliders.size();
    }

    /**
     * Casts a ray using the default query filter.
     *
     * @param origin world-space ray origin
     * @param direction non-zero ray direction; normalized internally
     * @param maximumDistance positive maximum world-space distance
     * @return nearest hit, or empty when nothing is reached
     */
    public Optional<RaycastHit> raycast(Vector3fc origin, Vector3fc direction, float maximumDistance) {
        return raycast(origin, direction, maximumDistance, QueryFilter.DEFAULT);
    }

    /**
     * Casts a ray and returns its nearest accepted hit.
     *
     * @param origin world-space ray origin
     * @param direction non-zero ray direction; normalized internally
     * @param maximumDistance positive maximum world-space distance
     * @param filter immutable query filter
     * @return nearest accepted hit, or empty when nothing is reached
     */
    public Optional<RaycastHit> raycast(
            Vector3fc origin, Vector3fc direction, float maximumDistance, QueryFilter filter) {
        Vector3f checkedOrigin = requireFinite(origin, "origin");
        Vector3f normalizedDirection = requireDirection(direction);
        Preconditions.requirePositive(maximumDistance, "maximumDistance");
        return queries.raycast(
                checkedOrigin, normalizedDirection, maximumDistance, Objects.requireNonNull(filter, "filter"));
    }

    /**
     * Finds overlaps using the default query filter.
     *
     * @param shape query shape
     * @param position world-space query position
     * @param orientation world-space query orientation; normalized internally
     * @return immutable hits ordered by collider identifier
     */
    public List<OverlapHit> overlap(CollisionShape shape, Vector3fc position, Quaternionfc orientation) {
        return overlap(shape, position, orientation, QueryFilter.DEFAULT);
    }

    /**
     * Finds every accepted collider overlapping the supplied shape pose.
     *
     * @param shape query shape
     * @param position world-space query position
     * @param orientation world-space query orientation; normalized internally
     * @param filter immutable query filter
     * @return immutable accepted hits ordered by collider identifier
     */
    public List<OverlapHit> overlap(
            CollisionShape shape, Vector3fc position, Quaternionfc orientation, QueryFilter filter) {
        return queries.overlap(new ShapePose(shape, position, orientation), Objects.requireNonNull(filter, "filter"));
    }

    /**
     * Sweeps a shape using the default query filter.
     *
     * @param shape query shape
     * @param position starting world-space position
     * @param orientation fixed world-space orientation; normalized internally
     * @param translation world-space translation over which to sweep
     * @return first hit, or empty when no collider is reached
     */
    public Optional<SweepHit> sweep(
            CollisionShape shape, Vector3fc position, Quaternionfc orientation, Vector3fc translation) {
        return sweep(shape, position, orientation, translation, QueryFilter.DEFAULT);
    }

    /**
     * Finds the first accepted collider reached while translating a shape.
     *
     * @param shape query shape
     * @param position starting world-space position
     * @param orientation fixed world-space orientation; normalized internally
     * @param translation world-space translation over which to sweep
     * @param filter immutable query filter
     * @return first accepted hit, or empty when no collider is reached
     */
    public Optional<SweepHit> sweep(
            CollisionShape shape,
            Vector3fc position,
            Quaternionfc orientation,
            Vector3fc translation,
            QueryFilter filter) {
        Vector3f checkedTranslation = requireFinite(translation, "translation");
        return queries.sweep(
                new ShapePose(shape, position, orientation),
                checkedTranslation,
                Objects.requireNonNull(filter, "filter"));
    }

    void updateTransform(Collider collider, Vector3fc position, Quaternionfc orientation) {
        requireOwnedAndRegistered(collider);
        ShapePose pose = new ShapePose(collider.shape(), position, orientation);
        collider.applyTransform(pose.position(new Vector3f()), pose.orientation(new Quaternionf()));
        index.update(collider, pose);
    }

    private void requireOwnedAndRegistered(Collider collider) {
        Objects.requireNonNull(collider, "collider");
        if (collider.world() != this || !collider.isRegistered()) {
            throw new IllegalArgumentException("collider is not registered with this world");
        }
    }

    private static Vector3f requireDirection(Vector3fc direction) {
        Vector3f checked = requireFinite(direction, "direction");
        if (checked.lengthSquared() < MINIMUM_DIRECTION_LENGTH_SQUARED) {
            throw new IllegalArgumentException("direction must be non-zero");
        }
        return checked.normalize();
    }

    private static Vector3f requireFinite(Vector3fc value, String name) {
        Objects.requireNonNull(value, name);
        if (!value.isFinite()) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3f(value);
    }
}
