/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Distance and direction between two non-overlapping shapes. */
final class SeparationResult {
    private final float distance;
    private final Vector3f normal;
    private final Vector3f point;

    SeparationResult(float distance, Vector3fc normal, Vector3fc point) {
        this.distance = distance;
        this.normal = new Vector3f(normal).normalize();
        this.point = new Vector3f(point);
    }

    float distance() {
        return distance;
    }

    Vector3f normal() {
        return new Vector3f(normal);
    }

    Vector3f point() {
        return new Vector3f(point);
    }
}
