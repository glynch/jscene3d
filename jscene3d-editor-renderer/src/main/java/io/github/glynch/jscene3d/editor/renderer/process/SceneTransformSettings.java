/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

/** Validated transform-tool state and snap increments supplied by the workbench. */
record SceneTransformSettings(
        SceneTransformTool tool,
        SceneTransformSpace space,
        boolean snapping,
        float translationSnap,
        float rotationSnap,
        float scaleSnap) {
    SceneTransformSettings {
        if (tool == null || space == null) {
            throw new NullPointerException("tool and space must be non-null");
        }
        requirePositive(translationSnap, "translationSnap");
        requirePositive(rotationSnap, "rotationSnap");
        requirePositive(scaleSnap, "scaleSnap");
    }

    static SceneTransformSettings defaults() {
        return new SceneTransformSettings(
                SceneTransformTool.SELECT, SceneTransformSpace.GLOBAL, false, 0.5f, 15.0f, 0.1f);
    }

    private static void requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }
}
