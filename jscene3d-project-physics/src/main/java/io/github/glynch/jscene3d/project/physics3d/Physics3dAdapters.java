/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.physics3d;

/** Creates world-scoped three-dimensional physics adapters. */
public final class Physics3dAdapters {
    /** Prevents construction of this stateless adapter factory. */
    private Physics3dAdapters() {
        throw new AssertionError("Physics3dAdapters cannot be instantiated");
    }

    /**
     * Creates the standard deterministic adapter backed by the renderer-independent JScene3D physics artifact.
     *
     * @return new open world adapter
     */
    public static Physics3dWorldModule standard() {
        return new StandardPhysics3dWorldModule();
    }
}
