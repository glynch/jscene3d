/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.objects.Object3D;

/** Immutable selected occurrence and transform baseline used to begin a transform drag. */
record SceneTransformSelection(
        VisualOccurrence occurrence, Object3D object, Transform3d transform, long revision, boolean editable) {
    Vector3 transformValue(SceneTransformTool tool) {
        return switch (tool) {
            case MOVE -> transform.position();
            case ROTATE -> transform.orientationDegrees();
            case SCALE -> transform.scale();
            case SELECT -> throw new IllegalArgumentException("Select has no transform value");
        };
    }
}
