/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.programs;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Robust direction calculations shared by built-in light upload paths. */
final class LightDirectionSupport {
    /** Prevents instantiation of this calculation utility class. */
    private LightDirectionSupport() {
        throw new AssertionError("LightDirectionSupport cannot be instantiated");
    }

    /**
     * Stores the normalized direction from one point toward another using double intermediates.
     *
     * @param from direction origin
     * @param to direction destination
     * @param destination mutable vector receiving the normalized result
     * @param zeroLengthMessage exception message when the points coincide
     */
    static void setNormalizedDifference(Vector3fc from, Vector3fc to, Vector3f destination, String zeroLengthMessage) {
        double x = (double) to.x() - from.x();
        double y = (double) to.y() - from.y();
        double z = (double) to.z() - from.z();
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0) {
            throw new IllegalStateException(zeroLengthMessage);
        }
        destination.set((float) (x / length), (float) (y / length), (float) (z / length));
    }
}
