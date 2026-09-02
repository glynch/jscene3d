/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics.internal;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Internal axis-aligned bounding box. */
final class Aabb {
    private static final float RAY_EPSILON = 1.0E-8F;

    private final Vector3f minimum;
    private final Vector3f maximum;

    Aabb(Vector3fc minimum, Vector3fc maximum) {
        this.minimum = new Vector3f(minimum);
        this.maximum = new Vector3f(maximum);
    }

    Vector3f minimum() {
        return minimum;
    }

    Vector3f maximum() {
        return maximum;
    }

    boolean overlaps(Aabb other) {
        return minimum.x <= other.maximum.x
                && maximum.x >= other.minimum.x
                && minimum.y <= other.maximum.y
                && maximum.y >= other.minimum.y
                && minimum.z <= other.maximum.z
                && maximum.z >= other.minimum.z;
    }

    boolean contains(Aabb other) {
        return minimum.x <= other.minimum.x
                && minimum.y <= other.minimum.y
                && minimum.z <= other.minimum.z
                && maximum.x >= other.maximum.x
                && maximum.y >= other.maximum.y
                && maximum.z >= other.maximum.z;
    }

    boolean intersectsRay(Vector3fc origin, Vector3fc direction, float maximumDistance) {
        float near = 0.0F;
        float far = maximumDistance;
        for (int axis = 0; axis < 3; axis++) {
            float directionComponent = direction.get(axis);
            if (Math.abs(directionComponent) >= RAY_EPSILON) {
                float directionInverse = 1.0F / directionComponent;
                float first = (minimum.get(axis) - origin.get(axis)) * directionInverse;
                float second = (maximum.get(axis) - origin.get(axis)) * directionInverse;
                float slabNear = Math.min(first, second);
                float slabFar = Math.max(first, second);
                near = Math.clamp(slabNear, near, Float.POSITIVE_INFINITY);
                far = Math.clamp(slabFar, Float.NEGATIVE_INFINITY, far);
            } else if (origin.get(axis) < minimum.get(axis) || origin.get(axis) > maximum.get(axis)) {
                return false;
            }
        }
        return near <= far;
    }

    float surfaceArea() {
        float width = maximum.x - minimum.x;
        float height = maximum.y - minimum.y;
        float depth = maximum.z - minimum.z;
        return 2.0F * (width * height + height * depth + depth * width);
    }

    Aabb expanded(float amount) {
        Vector3f expansion = new Vector3f(amount);
        return new Aabb(new Vector3f(minimum).sub(expansion), new Vector3f(maximum).add(expansion));
    }

    static Aabb combine(Aabb first, Aabb second) {
        return new Aabb(
                new Vector3f(first.minimum).min(second.minimum), new Vector3f(first.maximum).max(second.maximum));
    }
}
