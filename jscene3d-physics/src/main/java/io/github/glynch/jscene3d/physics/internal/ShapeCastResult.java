/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Internal result for a translating convex shape. */
final class ShapeCastResult {
    private final float fraction;
    private final Vector3f point;
    private final Vector3f normal;

    ShapeCastResult(float fraction, Vector3fc point, Vector3fc normal) {
        this.fraction = fraction;
        this.point = new Vector3f(point);
        this.normal = new Vector3f(normal).normalize();
    }

    float fraction() {
        return fraction;
    }

    Vector3f point() {
        return new Vector3f(point);
    }

    Vector3f normal() {
        return new Vector3f(normal);
    }
}
