/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Narrow-phase overlap information for the first shape relative to the second. */
final class ContactResult {
    private final float penetrationDepth;
    private final Vector3f normal;
    private final Vector3f point;

    ContactResult(float penetrationDepth, Vector3fc normal, Vector3fc point) {
        this.penetrationDepth = Math.max(0.0F, penetrationDepth);
        this.normal = new Vector3f(normal).normalize();
        this.point = new Vector3f(point);
    }

    /** Returns the minimum translation distance. */
    float penetrationDepth() {
        return penetrationDepth;
    }

    /** Copies the minimum translation direction into the destination. */
    Vector3f normal(Vector3f destination) {
        return destination.set(normal);
    }

    /** Copies an approximate world-space contact point into the destination. */
    Vector3f point(Vector3f destination) {
        return destination.set(point);
    }
}
