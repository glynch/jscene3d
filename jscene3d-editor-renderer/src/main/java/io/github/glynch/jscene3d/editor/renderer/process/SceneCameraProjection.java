/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneCameraIconState.Point;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Projects realized authored-camera positions through the active editor camera. */
final class SceneCameraProjection {
    private static final float MINIMUM_HOMOGENEOUS_W = 1.0e-6f;

    private SceneCameraProjection() {
        throw new AssertionError("SceneCameraProjection cannot be instantiated");
    }

    /** Projects one occurrence origin into visible logical-pixel coordinates. */
    static Optional<ProjectedPoint> project(Object3D occurrence, Camera editorCamera, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("viewport dimensions must be positive");
        }
        Vector4f clip = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
        occurrence.matrixWorld().transform(clip);
        new Matrix4f(editorCamera.viewMatrix()).transform(clip);
        editorCamera.projectionMatrix().transform(clip);
        if (!clip.isFinite() || clip.w() <= MINIMUM_HOMOGENEOUS_W) {
            return Optional.empty();
        }
        float reciprocalW = 1.0f / clip.w();
        float x = clip.x() * reciprocalW;
        float y = clip.y() * reciprocalW;
        float depth = clip.z() * reciprocalW;
        if (!Float.isFinite(x)
                || !Float.isFinite(y)
                || !Float.isFinite(depth)
                || x < -1.0f
                || x > 1.0f
                || y < -1.0f
                || y > 1.0f
                || depth < -1.0f
                || depth > 1.0f) {
            return Optional.empty();
        }
        return Optional.of(new ProjectedPoint(new Point((x + 1.0f) * 0.5f * width, (1.0f - y) * 0.5f * height), depth));
    }

    /** Logical-pixel point and OpenGL normalized depth. */
    record ProjectedPoint(Point point, float depth) {}
}
