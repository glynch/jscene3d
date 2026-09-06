/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

/** Creates world-scoped three-dimensional adapters without exposing their scene-graph implementation. */
public final class Spatial3dAdapters {
    /** Prevents construction of this stateless adapter factory. */
    private Spatial3dAdapters() {
        throw new AssertionError("Spatial3dAdapters cannot be instantiated");
    }

    /**
     * Creates the standard headless-capable adapter backed internally by JScene3D spatial objects.
     *
     * <p>Each composed world requires its own adapter instance. The world takes ownership only after successful
     * composition.
     *
     * @return new open spatial adapter
     */
    public static Spatial3dWorldModule standard() {
        return new Object3dSpatialAdapter();
    }
}
