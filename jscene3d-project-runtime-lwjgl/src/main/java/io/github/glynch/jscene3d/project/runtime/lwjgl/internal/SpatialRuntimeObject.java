/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.lwjgl.internal;

import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.runtime.lwjgl.Scene3dRuntimeObject;
import java.util.Objects;

/** Runtime object retaining one engine scene-graph object without owning shared resources. */
class SpatialRuntimeObject implements Scene3dRuntimeObject {
    private final Object3D object;

    /** Stores one scene-graph object. */
    SpatialRuntimeObject(Object3D object) {
        this.object = Objects.requireNonNull(object, "object");
    }

    @Override
    public final Object3D object3d() {
        return object;
    }

    @Override
    public void start() {
        // Static built-in nodes require no startup work.
    }

    @Override
    public void close() {
        object.detach();
    }
}
