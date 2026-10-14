/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import java.util.Objects;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Immutable renderer-authoritative editor-camera state for one Scene View session. */
record SceneViewCameraState(Vector3fc position, Vector3fc orientationDegrees, Vector3fc pivot, Projection projection) {
    SceneViewCameraState {
        position = copyFinite(position, "position");
        orientationDegrees = copyFinite(orientationDegrees, "orientationDegrees");
        pivot = copyFinite(pivot, "pivot");
        Objects.requireNonNull(projection, "projection");
    }

    private static Vector3fc copyFinite(Vector3fc value, String name) {
        Vector3fc validValue = Objects.requireNonNull(value, name);
        if (!Float.isFinite(validValue.x()) || !Float.isFinite(validValue.y()) || !Float.isFinite(validValue.z())) {
            throw new IllegalArgumentException(name + " must be finite");
        }
        return new Vector3f(validValue);
    }

    /** Active projection used to render the Scene View. */
    enum Projection {
        PERSPECTIVE,
        ORTHOGRAPHIC
    }
}
