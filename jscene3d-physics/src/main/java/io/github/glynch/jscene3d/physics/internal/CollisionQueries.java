/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.Collider;
import io.github.glynch.jscene3d.physics.queries.OverlapHit;
import io.github.glynch.jscene3d.physics.queries.QueryFilter;
import io.github.glynch.jscene3d.physics.queries.RaycastHit;
import io.github.glynch.jscene3d.physics.queries.SweepHit;
import io.github.glynch.jscene3d.physics.queries.TriggerMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Narrow public-facing facade over broad- and narrow-phase query machinery. */
public final class CollisionQueries {
    private final WorldIndex index;

    /**
     * Creates queries backed by the supplied world index.
     *
     * @param index broad-phase world index
     */
    public CollisionQueries(WorldIndex index) {
        this.index = index;
    }

    /**
     * Finds the closest accepted ray hit.
     *
     * @param origin world-space ray origin
     * @param direction normalized ray direction
     * @param maximumDistance maximum world-space distance
     * @param filter query filter
     * @return closest accepted hit, if present
     */
    public Optional<RaycastHit> raycast(
            Vector3fc origin, Vector3fc direction, float maximumDistance, QueryFilter filter) {
        List<RayCandidate> hits = new ArrayList<>();
        for (Collider collider : sorted(index.rayCandidates(origin, direction, maximumDistance))) {
            if (!accepts(collider, filter)) {
                continue;
            }
            RayIntersections.intersect(origin, direction, maximumDistance, pose(collider))
                    .ifPresent(hit -> hits.add(new RayCandidate(collider, hit)));
        }
        return hits.stream()
                .min(Comparator.comparingDouble(
                                (RayCandidate candidate) -> candidate.hit().distance())
                        .thenComparingLong(candidate -> candidate.collider().id()))
                .map(candidate -> new RaycastHit(
                        candidate.collider(),
                        candidate.hit().distance(),
                        candidate.hit().point(),
                        candidate.hit().normal()));
    }

    /**
     * Finds all accepted colliders overlapping the query pose.
     *
     * @param queryPose query shape and transform
     * @param filter query filter
     * @return immutable accepted overlap hits
     */
    public List<OverlapHit> overlap(ShapePose queryPose, QueryFilter filter) {
        List<OverlapHit> hits = new ArrayList<>();
        for (Collider collider : sorted(index.overlapCandidates(queryPose))) {
            if (!accepts(collider, filter)) {
                continue;
            }
            OverlapTests.contact(queryPose, pose(collider))
                    .ifPresent(contact -> hits.add(
                            new OverlapHit(collider, contact.penetrationDepth(), contact.normal(new Vector3f()))));
        }
        return List.copyOf(hits);
    }

    /**
     * Finds the first accepted collider reached by a translating shape.
     *
     * @param queryPose starting query shape and transform
     * @param translation world-space translation
     * @param filter query filter
     * @return first accepted sweep hit, if present
     */
    public Optional<SweepHit> sweep(ShapePose queryPose, Vector3fc translation, QueryFilter filter) {
        List<SweepCandidate> hits = new ArrayList<>();
        for (Collider collider : sorted(index.sweepCandidates(queryPose, translation))) {
            if (!accepts(collider, filter)) {
                continue;
            }
            ShapeCast.cast(queryPose, translation, pose(collider))
                    .ifPresent(hit -> hits.add(new SweepCandidate(collider, hit)));
        }
        float length = translation.length();
        return hits.stream()
                .min(Comparator.comparingDouble(
                                (SweepCandidate candidate) -> candidate.hit().fraction())
                        .thenComparingLong(candidate -> candidate.collider().id()))
                .map(candidate -> new SweepHit(
                        candidate.collider(),
                        candidate.hit().fraction(),
                        candidate.hit().fraction() * length,
                        candidate.hit().point(),
                        candidate.hit().normal()));
    }

    private static ShapePose pose(Collider collider) {
        return new ShapePose(
                collider.shape(), collider.position(new Vector3f()), collider.orientation(new Quaternionf()));
    }

    private static boolean accepts(Collider collider, QueryFilter filter) {
        if (!collider.isRegistered() || !collider.isEnabled()) {
            return false;
        }
        if ((filter.layerMask() & collider.collisionFilter().categoryBits()) == 0) {
            return false;
        }
        if (filter.excludedCollider().filter(excluded -> excluded == collider).isPresent()) {
            return false;
        }
        return acceptsTrigger(collider, filter.triggerMode());
    }

    private static boolean acceptsTrigger(Collider collider, TriggerMode triggerMode) {
        return switch (triggerMode) {
            case EXCLUDE -> !collider.isTrigger();
            case INCLUDE -> true;
            case ONLY -> collider.isTrigger();
        };
    }

    private static List<Collider> sorted(List<Collider> colliders) {
        return colliders.stream().sorted(Comparator.comparingLong(Collider::id)).toList();
    }

    private record RayCandidate(Collider collider, RayHitResult hit) {}

    private record SweepCandidate(Collider collider, ShapeCastResult hit) {}
}
