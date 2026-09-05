/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import io.github.glynch.jscene3d.physics.shapes.TriangleMeshShape;
import java.util.Optional;
import java.util.function.Predicate;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Conservative advancement for convex shapes with fixed orientation. */
final class ShapeCast {
    private static final int MAXIMUM_ITERATIONS = 32;
    private static final float DISTANCE_TOLERANCE = 1.0E-4F;
    private static final float CLOSING_SPEED_EPSILON = 1.0E-7F;

    private ShapeCast() {}

    static Optional<ShapeCastResult> cast(ShapePose moving, Vector3fc translation, ShapePose target) {
        return cast(moving, translation, target, ignored -> true);
    }

    /** Casts against the accepted features of one target shape. */
    static Optional<ShapeCastResult> cast(
            ShapePose moving, Vector3fc translation, ShapePose target, Predicate<ShapeCastResult> acceptance) {
        if (target.shape() instanceof TriangleMeshShape mesh) {
            return TriangleMeshQueries.cast(moving, translation, mesh, target, acceptance);
        }
        return castFeature(
                        moving,
                        translation,
                        current -> OverlapTests.contact(current, target),
                        current -> SeparationTests.between(current, target))
                .filter(acceptance);
    }

    /** Conservatively advances against one independently testable target feature. */
    static Optional<ShapeCastResult> castFeature(
            ShapePose moving, Vector3fc translation, ContactQuery contactQuery, SeparationQuery separationQuery) {
        float fraction = 0.0F;
        Vector3f start = moving.position(new Vector3f());
        Quaternionf orientation = moving.orientation(new Quaternionf());
        for (int iteration = 0; iteration < MAXIMUM_ITERATIONS; iteration++) {
            ShapePose current = new ShapePose(
                    moving.shape(), new Vector3f(translation).mul(fraction).add(start), orientation);
            Optional<ContactResult> contact = contactQuery.find(current);
            if (contact.isPresent()) {
                ContactResult value = contact.orElseThrow();
                return Optional.of(
                        new ShapeCastResult(fraction, value.point(new Vector3f()), value.normal(new Vector3f())));
            }
            SeparationResult separation = separationQuery.find(current);
            if (separation.distance() <= DISTANCE_TOLERANCE) {
                return Optional.of(new ShapeCastResult(fraction, separation.point(), separation.normal()));
            }
            float closingSpeed = -translation.dot(separation.normal());
            if (closingSpeed <= CLOSING_SPEED_EPSILON) {
                return Optional.empty();
            }
            float advance = separation.distance() / closingSpeed;
            if (advance <= DISTANCE_TOLERANCE) {
                return Optional.of(new ShapeCastResult(fraction, separation.point(), separation.normal()));
            }
            fraction += advance;
            if (fraction > 1.0F) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    /** Finds contact between the moving shape and one target feature. */
    @FunctionalInterface
    interface ContactQuery {
        Optional<ContactResult> find(ShapePose moving);
    }

    /** Finds separation between the moving shape and one target feature. */
    @FunctionalInterface
    interface SeparationQuery {
        SeparationResult find(ShapePose moving);
    }
}
