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
import io.github.glynch.jscene3d.physics.movement.OverlapEvent;
import io.github.glynch.jscene3d.physics.movement.OverlapPhase;
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

/** Owns collision objects and answers deterministic three-dimensional collision queries. */
public final class PhysicsWorld {
    private static final Vector3fc ZERO = new Vector3f();
    private static final Quaternionfc IDENTITY = new Quaternionf();

    private final Set<CollisionObject> collisionObjects = new LinkedHashSet<>();
    private final Set<Collider> colliders = new LinkedHashSet<>();
    private final WorldIndex index = new WorldIndex();
    private final CollisionQueries queries = new CollisionQueries(index);
    private final KinematicMovement movement = new KinematicMovement(queries);
    private final Map<KinematicBody, Set<CollisionSensor>> activeOverlaps = new IdentityHashMap<>();
    private long nextObjectId;
    private long nextColliderId;

    /** Creates an empty physics world. */
    public PhysicsWorld() {
        nextObjectId = 1L;
        nextColliderId = 1L;
    }

    /**
     * Adds a static body at the world origin.
     *
     * @return world-owned static body
     */
    public StaticBody addStaticBody() {
        return addStaticBody(ZERO, IDENTITY);
    }

    /**
     * Adds an immovable body at the supplied world transform.
     *
     * @param position world-space position
     * @param orientation world-space orientation; normalized internally
     * @return world-owned static body
     */
    public StaticBody addStaticBody(Vector3fc position, Quaternionfc orientation) {
        ObjectTransform transform = validatedTransform(position, orientation);
        StaticBody body = new StaticBody(this, nextObjectId++, transform.position(), transform.orientation());
        collisionObjects.add(body);
        return body;
    }

    /**
     * Adds a kinematic body at the world origin.
     *
     * @return world-owned kinematic body
     */
    public KinematicBody addKinematicBody() {
        return addKinematicBody(ZERO, IDENTITY);
    }

    /**
     * Adds a caller-moved body at the supplied world transform.
     *
     * @param position world-space position
     * @param orientation world-space orientation; normalized internally
     * @return world-owned kinematic body
     */
    public KinematicBody addKinematicBody(Vector3fc position, Quaternionfc orientation) {
        ObjectTransform transform = validatedTransform(position, orientation);
        KinematicBody body = new KinematicBody(this, nextObjectId++, transform.position(), transform.orientation());
        collisionObjects.add(body);
        return body;
    }

    /**
     * Adds a collision sensor at the world origin.
     *
     * @return world-owned collision sensor
     */
    public CollisionSensor addCollisionSensor() {
        return addCollisionSensor(ZERO, IDENTITY);
    }

    /**
     * Adds a non-blocking collision sensor at the supplied world transform.
     *
     * @param position world-space position
     * @param orientation world-space orientation; normalized internally
     * @return world-owned collision sensor
     */
    public CollisionSensor addCollisionSensor(Vector3fc position, Quaternionfc orientation) {
        ObjectTransform transform = validatedTransform(position, orientation);
        CollisionSensor sensor =
                new CollisionSensor(this, nextObjectId++, transform.position(), transform.orientation());
        collisionObjects.add(sensor);
        return sensor;
    }

    /**
     * Removes a collision object and all of its colliders.
     *
     * @param collisionObject registered object owned by this world
     */
    public void remove(CollisionObject collisionObject) {
        requireOwnedAndRegistered(collisionObject);
        List.copyOf(collisionObject.colliders()).forEach(collider -> removeCollider(collisionObject, collider));
        collisionObjects.remove(collisionObject);
        if (collisionObject instanceof KinematicBody body) {
            activeOverlaps.remove(body);
        }
        if (collisionObject instanceof CollisionSensor sensor) {
            activeOverlaps.values().forEach(overlaps -> overlaps.remove(sensor));
        }
        collisionObject.markRemoved();
    }

    /** Removes every collision object and invalidates every outstanding handle. */
    public void clear() {
        collisionObjects.forEach(CollisionObject::markRemoved);
        collisionObjects.clear();
        colliders.clear();
        index.clear();
        activeOverlaps.clear();
    }

    /**
     * Returns the number of registered collision objects.
     *
     * @return registered collision-object count
     */
    public int collisionObjectCount() {
        return collisionObjects.size();
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
        Vector3f checkedOrigin = Preconditions.requireFinite(origin, "origin");
        Vector3f normalizedDirection = Preconditions.requireDirection(direction, "direction");
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
        Vector3f checkedTranslation = Preconditions.requireFinite(translation, "translation");
        return queries.sweep(
                new ShapePose(shape, position, orientation),
                checkedTranslation,
                Objects.requireNonNull(filter, "filter"));
    }

    /**
     * Moves a registered kinematic body using the default settings.
     *
     * @param body registered kinematic body to move
     * @param translation desired world-space translation
     * @return immutable resolved movement result
     */
    public KinematicMoveResult move(KinematicBody body, Vector3fc translation) {
        return move(body, translation, KinematicMoveSettings.DEFAULT);
    }

    /**
     * Moves a registered kinematic body with explicit collision-resolution settings.
     *
     * @param body registered kinematic body to move
     * @param translation desired world-space translation
     * @param settings immutable collision-resolution settings
     * @return immutable resolved movement result
     */
    public KinematicMoveResult move(KinematicBody body, Vector3fc translation, KinematicMoveSettings settings) {
        requireOwnedAndRegistered(body);
        KinematicMoveResult resolved =
                movement.resolve(body, translation, Objects.requireNonNull(settings, "settings"));
        Vector3f position = body.position(new Vector3f()).add(resolved.appliedTranslation(new Vector3f()));
        updateTransform(body, position, body.orientation(new Quaternionf()));
        return resolved.withOverlapEvents(updateOverlaps(body));
    }

    Collider addCollider(
            CollisionObject collisionObject,
            CollisionShape shape,
            Vector3fc localPosition,
            Quaternionfc localOrientation) {
        requireOwnedAndRegistered(collisionObject);
        ShapePose localPose = new ShapePose(shape, localPosition, localOrientation);
        ShapePose worldPose = worldPose(collisionObject, localPose);
        Collider collider = new Collider(
                collisionObject,
                nextColliderId++,
                shape,
                localPose.position(new Vector3f()),
                localPose.orientation(new Quaternionf()),
                worldPose.position(new Vector3f()),
                worldPose.orientation(new Quaternionf()));
        collisionObject.attach(collider);
        colliders.add(collider);
        index.add(collider, worldPose);
        return collider;
    }

    void removeCollider(CollisionObject collisionObject, Collider collider) {
        requireOwnedAndRegistered(collisionObject);
        Objects.requireNonNull(collider, "collider");
        if (collider.collisionObject() != collisionObject || !collider.isRegistered()) {
            throw new IllegalArgumentException("collider is not registered with this collision object");
        }
        index.remove(collider);
        colliders.remove(collider);
        collisionObject.detach(collider);
        collider.markRemoved();
    }

    void updateTransform(CollisionObject collisionObject, Vector3fc position, Quaternionfc orientation) {
        requireOwnedAndRegistered(collisionObject);
        ObjectTransform transform = validatedTransform(position, orientation);
        collisionObject.applyTransform(transform.position(), transform.orientation());
        for (Collider collider : collisionObject.colliders()) {
            ShapePose pose = worldPose(collisionObject, localPose(collider));
            collider.applyTransform(pose.position(new Vector3f()), pose.orientation(new Quaternionf()));
            index.update(collider, pose);
        }
    }

    private void requireOwnedAndRegistered(CollisionObject collisionObject) {
        Objects.requireNonNull(collisionObject, "collisionObject");
        if (collisionObject.world() != this || !collisionObject.isRegistered()) {
            throw new IllegalArgumentException("collision object is not registered with this world");
        }
    }

    private List<OverlapEvent> updateOverlaps(KinematicBody body) {
        Set<CollisionSensor> current = new LinkedHashSet<>();
        for (Collider movingCollider : body.colliders()) {
            if (!movingCollider.isEnabled()) {
                continue;
            }
            queries
                    .overlapAccepted(worldPose(movingCollider), candidate -> acceptsSensor(movingCollider, candidate))
                    .stream()
                    .map(OverlapHit::collisionObject)
                    .map(CollisionSensor.class::cast)
                    .forEach(current::add);
        }
        Set<CollisionSensor> previous = activeOverlaps.getOrDefault(body, Set.of());
        List<OverlapEvent> events = new ArrayList<>();
        current.stream()
                .sorted(Comparator.comparingLong(CollisionObject::id))
                .map(sensor ->
                        new OverlapEvent(sensor, previous.contains(sensor) ? OverlapPhase.STAY : OverlapPhase.ENTER))
                .forEach(events::add);
        previous.stream()
                .filter(sensor -> !current.contains(sensor))
                .sorted(Comparator.comparingLong(CollisionObject::id))
                .map(sensor -> new OverlapEvent(sensor, OverlapPhase.EXIT))
                .forEach(events::add);
        if (current.isEmpty()) {
            activeOverlaps.remove(body);
        } else {
            activeOverlaps.put(body, current);
        }
        return List.copyOf(events);
    }

    private static boolean acceptsSensor(Collider movingCollider, Collider candidate) {
        return candidate.collisionObject() instanceof CollisionSensor
                && candidate.isRegistered()
                && candidate.isEnabled()
                && candidate.collisionObject().isEnabled()
                && movingCollider.collisionFilter().matches(candidate.collisionFilter());
    }

    private static ShapePose localPose(Collider collider) {
        return new ShapePose(
                collider.shape(), collider.localPosition(new Vector3f()), collider.localOrientation(new Quaternionf()));
    }

    private static ShapePose worldPose(Collider collider) {
        return new ShapePose(
                collider.shape(), collider.position(new Vector3f()), collider.orientation(new Quaternionf()));
    }

    private static ShapePose worldPose(CollisionObject collisionObject, ShapePose localPose) {
        Quaternionf objectOrientation = collisionObject.orientation(new Quaternionf());
        Vector3f position = objectOrientation
                .transform(localPose.position(new Vector3f()))
                .add(collisionObject.position(new Vector3f()));
        Quaternionf orientation = objectOrientation.mul(localPose.orientation(new Quaternionf()), new Quaternionf());
        return new ShapePose(localPose.shape(), position, orientation);
    }

    private static ObjectTransform validatedTransform(Vector3fc position, Quaternionfc orientation) {
        return new ObjectTransform(
                Preconditions.requireFinite(position, "position"),
                Preconditions.requireOrientation(orientation, "orientation"));
    }

    private record ObjectTransform(Vector3f position, Quaternionf orientation) {}
}
