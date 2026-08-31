/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.math;

import io.github.glynch.jscene3d.internal.Preconditions;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable axis-aligned three-dimensional bounds. */
public final class BoundingBox {
    private final Vector3f minimum;
    private final Vector3f maximum;

    /**
     * Creates bounds from finite minimum and maximum coordinates.
     *
     * @param minimumX minimum X coordinate
     * @param minimumY minimum Y coordinate
     * @param minimumZ minimum Z coordinate
     * @param maximumX maximum X coordinate
     * @param maximumY maximum Y coordinate
     * @param maximumZ maximum Z coordinate
     * @throws IllegalArgumentException if a coordinate is not finite or a minimum exceeds its
     *     corresponding maximum
     */
    public BoundingBox(float minimumX, float minimumY, float minimumZ, float maximumX, float maximumY, float maximumZ) {
        float validMinimumX = Preconditions.requireFinite(minimumX, "minimumX");
        float validMinimumY = Preconditions.requireFinite(minimumY, "minimumY");
        float validMinimumZ = Preconditions.requireFinite(minimumZ, "minimumZ");
        float validMaximumX = Preconditions.requireFinite(maximumX, "maximumX");
        float validMaximumY = Preconditions.requireFinite(maximumY, "maximumY");
        float validMaximumZ = Preconditions.requireFinite(maximumZ, "maximumZ");
        requireOrdered(validMinimumX, validMaximumX, "X");
        requireOrdered(validMinimumY, validMaximumY, "Y");
        requireOrdered(validMinimumZ, validMaximumZ, "Z");
        minimum = new Vector3f(validMinimumX, validMinimumY, validMinimumZ);
        maximum = new Vector3f(validMaximumX, validMaximumY, validMaximumZ);
    }

    /**
     * Returns the stable read-only minimum corner.
     *
     * @return the minimum corner
     */
    public Vector3fc minimum() {
        return minimum;
    }

    /**
     * Returns the stable read-only maximum corner.
     *
     * @return the maximum corner
     */
    public Vector3fc maximum() {
        return maximum;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof BoundingBox box && minimum.equals(box.minimum) && maximum.equals(box.maximum));
    }

    @Override
    public int hashCode() {
        int result = minimum.hashCode();
        return 31 * result + maximum.hashCode();
    }

    @Override
    public String toString() {
        return "BoundingBox[minimum=" + minimum + ", maximum=" + maximum + ']';
    }

    /** Requires ordered bounds for one named axis. */
    private static void requireOrdered(float minimum, float maximum, String axis) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                    "minimum" + axis + " must not exceed maximum" + axis + ": " + minimum + " > " + maximum);
        }
    }
}
