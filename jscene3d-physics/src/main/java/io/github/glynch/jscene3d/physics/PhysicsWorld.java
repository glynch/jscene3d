/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import io.github.glynch.jscene3d.physics.debug.PhysicsDebugSnapshot;
import io.github.glynch.jscene3d.physics.internal.CollisionQueries;
import io.github.glynch.jscene3d.physics.internal.DebugGeometry;
import io.github.glynch.jscene3d.physics.internal.KinematicMovement;
import io.github.glynch.jscene3d.physics.internal.Preconditions;
import io.github.glynch.jscene3d.physics.internal.ShapePose;
import io.github.glynch.jscene3d.physics.internal.WorldIndex;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveResult;
import io.github.glynch.jscene3d.physics.movement.KinematicMoveSettings;
import io.github.glynch.jscene3d.physics.movement.TriggerEvent;
import io.github.glynch.jscene3d.physics.movement.TriggerEventType;
import io.github.glynch.jscene3d.physics.queries.OverlapHit;
import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.RaycastHit;
import io.github.glynch.jscene3d.physics.queries.SweepHit;
import io.github.glynch.jscene3d.physics.shapes.CollisionShape;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    private final KinematicMovement movement;
    private final Map<Collider, Set<Collider>> activeTriggers = new IdentityHashMap<>();
    private long nextColliderId = 1L;

    /** Creates an empty physics world. */
    public PhysicsWorld() {
        colliders = new LinkedHashSet<>();
        index = new WorldIndex();
        queries = new CollisionQueries(index);
        movement = new KinematicMovement(queries);
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
        activeTriggers.remove(collider);
        collider.markRemoved();
    }

    /** Removes every collider and invalidates every outstanding handle. */
    public void clear() {
        for (Collider collider : colliders) {
            collider.markRemoved();
        }
        colliders.clear();
        index.clear();
        activeTriggers.clear();
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
     * Captures renderer-independent line geometry for every registered collider.
     *
     * @return immutable debug snapshot ordered by collider identifier
     */
    public PhysicsDebugSnapshot debugSnapshot() {
        return DebugGeometry.snapshot(colliders);
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

    /**
     * Moves a registered collider using the default kinematic settings.
     *
     * <p>The requested translation is caller-owned and normally includes velocity, gravity, and
     * other game-specific intent accumulated for one fixed update. The world applies the resolved
     * transform immediately and reports contacts, grounding, step traversal, and trigger changes.
     *
     * @param collider registered collider to move
     * @param translation desired world-space translation
     * @return immutable resolved movement result
     */
    public KinematicMoveResult move(Collider collider, Vector3fc translation) {
        return move(collider, translation, KinematicMoveSettings.DEFAULT);
    }

    /**
     * Moves a registered collider with explicit collision-resolution settings.
     *
     * @param collider registered collider to move
     * @param translation desired world-space translation
     * @param settings immutable collision-resolution settings
     * @return immutable resolved movement result
     */
    public KinematicMoveResult move(Collider collider, Vector3fc translation, KinematicMoveSettings settings) {
        requireOwnedAndRegistered(collider);
        KinematicMoveResult resolved =
                movement.resolve(collider, translation, Objects.requireNonNull(settings, "settings"));
        Vector3f position = collider.position(new Vector3f()).add(resolved.appliedTranslation(new Vector3f()));
        updateTransform(collider, position, collider.orientation(new Quaternionf()));
        List<TriggerEvent> triggerEvents = updateTriggers(collider);
        return resolved.withTriggerEvents(triggerEvents);
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

    private List<TriggerEvent> updateTriggers(Collider movingCollider) {
        ShapePose pose = new ShapePose(
                movingCollider.shape(),
                movingCollider.position(new Vector3f()),
                movingCollider.orientation(new Quaternionf()));
        Set<Collider> current = new LinkedHashSet<>();
        queries.overlapAccepted(pose, candidate -> acceptsTrigger(movingCollider, candidate)).stream()
                .map(OverlapHit::collider)
                .forEach(current::add);
        Set<Collider> previous = activeTriggers.getOrDefault(movingCollider, Set.of());
        List<TriggerEvent> events = new ArrayList<>();
        current.stream()
                .sorted(Comparator.comparingLong(Collider::id))
                .map(trigger -> new TriggerEvent(
                        trigger, previous.contains(trigger) ? TriggerEventType.STAY : TriggerEventType.ENTER))
                .forEach(events::add);
        previous.stream()
                .filter(trigger -> !current.contains(trigger))
                .sorted(Comparator.comparingLong(Collider::id))
                .map(trigger -> new TriggerEvent(trigger, TriggerEventType.EXIT))
                .forEach(events::add);
        if (current.isEmpty()) {
            activeTriggers.remove(movingCollider);
        } else {
            activeTriggers.put(movingCollider, current);
        }
        return List.copyOf(events);
    }

    private static boolean acceptsTrigger(Collider movingCollider, Collider candidate) {
        return candidate != movingCollider
                && candidate.isRegistered()
                && candidate.isEnabled()
                && candidate.isTrigger()
                && movingCollider.collisionFilter().matches(candidate.collisionFilter());
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
