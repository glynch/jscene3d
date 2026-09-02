/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Internal ray intersection result. */
final class RayHitResult {
    private final float distance;
    private final Vector3f point;
    private final Vector3f normal;

    RayHitResult(float distance, Vector3fc point, Vector3fc normal) {
        this.distance = distance;
        this.point = new Vector3f(point);
        this.normal = new Vector3f(normal).normalize();
    }

    float distance() {
        return distance;
    }

    Vector3f point() {
        return new Vector3f(point);
    }

    Vector3f normal() {
        return new Vector3f(normal);
    }
}
