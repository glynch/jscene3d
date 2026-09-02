/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import java.util.Optional;
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
        float fraction = 0.0F;
        Vector3f start = moving.position(new Vector3f());
        Quaternionf orientation = moving.orientation(new Quaternionf());
        for (int iteration = 0; iteration < MAXIMUM_ITERATIONS; iteration++) {
            ShapePose current = new ShapePose(
                    moving.shape(), new Vector3f(translation).mul(fraction).add(start), orientation);
            Optional<ContactResult> contact = OverlapTests.contact(current, target);
            if (contact.isPresent()) {
                ContactResult value = contact.orElseThrow();
                return Optional.of(
                        new ShapeCastResult(fraction, value.point(new Vector3f()), value.normal(new Vector3f())));
            }
            SeparationResult separation = SeparationTests.between(current, target);
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
}
