/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static org.joml.Math.toRadians;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Demonstrates automatic perspective, orthographic, and view matrices. */
public final class CamerasExample {
    private static final float EPSILON = 1.0e-5f;

    /** Prevents instantiation of this example entry point. */
    private CamerasExample() {
        throw new AssertionError("CamerasExample cannot be instantiated");
    }

    /**
     * Creates both version 0.1 camera types and verifies their central viewing direction.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        PerspectiveCamera perspective = new PerspectiveCamera(toRadians(60.0f), 16.0f / 9.0f, 0.1f, 100.0f);
        perspective.setPosition(3.0f, 2.0f, 5.0f);
        perspective.lookAt(0.0f, 0.0f, 0.0f);

        Vector3f projectedOrigin = new Matrix4f(perspective.projectionMatrix())
                .mul(perspective.viewMatrix())
                .project(0.0f, 0.0f, 0.0f, new int[] {0, 0, 2, 2}, new Vector3f());
        if (!projectedOrigin.equals(new Vector3f(1.0f, 1.0f, projectedOrigin.z), EPSILON)) {
            throw new IllegalStateException("Perspective camera did not center its target: " + projectedOrigin);
        }

        OrthographicCamera orthographic = new OrthographicCamera(-2.0f, 2.0f, 2.0f, -2.0f, 0.0f, 100.0f);
        orthographic.setPosition(0.0f, 0.0f, 5.0f);
        Vector3f viewedOrigin = orthographic.viewMatrix().transformPosition(new Vector3f());
        if (!viewedOrigin.equals(new Vector3f(0.0f, 0.0f, -5.0f), EPSILON)) {
            throw new IllegalStateException("Unexpected orthographic view position: " + viewedOrigin);
        }
    }
}
