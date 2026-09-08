/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom.geometry;

/**
 * Player-one camera start expressed in JScene3D world coordinates.
 *
 * @param x world-space horizontal coordinate
 * @param eyeHeight world-space camera height
 * @param z world-space depth coordinate
 * @param yawRadians horizontal orientation in radians
 */
public record DoomPlayerStart(float x, float eyeHeight, float z, float yawRadians) {
    /** Creates a finite player start. */
    public DoomPlayerStart {
        requireFinite(x, "x");
        requireFinite(eyeHeight, "eyeHeight");
        requireFinite(z, "z");
        requireFinite(yawRadians, "yawRadians");
    }

    /** Rejects non-finite spatial values at the immutable model boundary. */
    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
