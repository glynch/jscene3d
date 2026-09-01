/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.lights;

import io.github.glynch.jscene3d.internal.Preconditions;

/** Orthographic shadow-camera configuration owned by a {@link DirectionalLight}. */
public final class DirectionalLightShadow extends LightShadow {
    private float cameraLeft;
    private float cameraRight;
    private float cameraBottom;
    private float cameraTop;

    /** Creates a 10-by-10 orthographic shadow volume with the common shadow defaults. */
    DirectionalLightShadow() {
        super(false);
        cameraLeft = -5.0f;
        cameraRight = 5.0f;
        cameraBottom = -5.0f;
        cameraTop = 5.0f;
    }

    /**
     * Returns the shadow camera's left plane.
     *
     * @return finite left coordinate, initially {@code -5}
     */
    public float cameraLeft() {
        return cameraLeft;
    }

    /**
     * Returns the shadow camera's right plane.
     *
     * @return finite right coordinate greater than {@link #cameraLeft()}, initially {@code 5}
     */
    public float cameraRight() {
        return cameraRight;
    }

    /**
     * Returns the shadow camera's bottom plane.
     *
     * @return finite bottom coordinate, initially {@code -5}
     */
    public float cameraBottom() {
        return cameraBottom;
    }

    /**
     * Returns the shadow camera's top plane.
     *
     * @return finite top coordinate greater than {@link #cameraBottom()}, initially {@code 5}
     */
    public float cameraTop() {
        return cameraTop;
    }

    /**
     * Changes the orthographic shadow-camera bounds atomically.
     *
     * @param left finite left coordinate
     * @param right finite coordinate greater than {@code left}
     * @param bottom finite bottom coordinate
     * @param top finite coordinate greater than {@code bottom}
     * @throws IllegalArgumentException if either interval is invalid
     */
    public void setCameraBounds(float left, float right, float bottom, float top) {
        Preconditions.requireLessThan(left, "left", right, "right");
        Preconditions.requireLessThan(bottom, "bottom", top, "top");
        cameraLeft = left;
        cameraRight = right;
        cameraBottom = bottom;
        cameraTop = top;
    }
}
