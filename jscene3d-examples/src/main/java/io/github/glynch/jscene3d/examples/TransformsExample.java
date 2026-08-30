/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples;

import static org.joml.Math.PI_OVER_2_f;

import io.github.glynch.jscene3d.core.Object3D;
import org.joml.Vector3f;

/** Demonstrates automatic local and inherited world transforms. */
public final class TransformsExample {
    private static final float EPSILON = 1.0e-5f;

    /** Prevents instantiation of this example entry point. */
    private TransformsExample() {
        throw new AssertionError("TransformsExample cannot be instantiated");
    }

    /**
     * Builds a transformed hierarchy and verifies its world position.
     *
     * @param arguments ignored command-line arguments
     */
    public static void main(String[] arguments) {
        Object3D planetarySystem = new Object3D();
        Object3D planet = new Object3D();
        planetarySystem.add(planet);

        planetarySystem.setPosition(10.0f, 0.0f, 0.0f);
        planetarySystem.rotateZ(PI_OVER_2_f);
        planet.setPosition(new Vector3f(2.0f, 0.0f, 0.0f));
        planet.setScale(0.5f, 0.5f, 0.5f);

        Vector3f worldPosition = planet.worldPosition(new Vector3f());
        Vector3f expectedPosition = new Vector3f(10.0f, 2.0f, 0.0f);
        if (!worldPosition.equals(expectedPosition, EPSILON)) {
            throw new IllegalStateException("Unexpected inherited world position: " + worldPosition);
        }

        planetarySystem.setPosition(20.0f, 0.0f, 0.0f);
        planet.worldPosition(worldPosition);
        expectedPosition.set(20.0f, 2.0f, 0.0f);
        if (!worldPosition.equals(expectedPosition, EPSILON)) {
            throw new IllegalStateException("World position was not updated after moving the parent");
        }
    }
}
