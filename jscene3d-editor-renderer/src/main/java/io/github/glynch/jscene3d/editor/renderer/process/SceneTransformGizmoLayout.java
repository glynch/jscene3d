/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.AxisVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Point;
import io.github.glynch.jscene3d.objects.Object3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/** Projects one selected transform into stable logical screen-space gizmo geometry. */
final class SceneTransformGizmoLayout {
    private final Supplier<? extends Camera> camera;
    private final Vector3f origin = new Vector3f();
    private final Vector3f projected = new Vector3f();
    private final Vector4f clip = new Vector4f();

    /** Retains the session-local editor camera supplier. */
    SceneTransformGizmoLayout(Supplier<? extends Camera> camera) {
        this.camera = camera;
    }

    /** Builds a frame for one selected object, coordinate space, and viewport. */
    Optional<SceneTransformGizmoState> create(Object3D object, SceneTransformSettings settings, int width, int height) {
        if (settings.tool() == SceneTransformTool.SELECT || width <= 0 || height <= 0) {
            return Optional.empty();
        }
        object.worldPosition(origin);
        Point center = project(origin, width, height);
        if (center == null) {
            return Optional.empty();
        }
        Quaternionf local = object.worldQuaternion(new Quaternionf());
        List<Vector3f> worldAxes = new ArrayList<>(3);
        for (Axis axis : Axis.values()) {
            Vector3f world = axis.vector();
            if (settings.space() == SceneTransformSpace.LOCAL) {
                local.transform(world);
            }
            worldAxes.add(world);
        }
        List<AxisVisual> axes = new ArrayList<>(3);
        for (Axis axis : Axis.values()) {
            Vector3f world = worldAxes.get(axis.ordinal());
            projected.set(world);
            camera.get().viewMatrix().transformDirection(projected);
            float length = (float) Math.hypot(projected.x, projected.y);
            float directionX = length < 0.05f ? 0.0f : projected.x / length;
            float directionY = length < 0.05f ? 0.0f : -projected.y / length;
            Point first = projectDirection(worldAxes.get(firstRingAxis(axis).ordinal()));
            Point second = projectDirection(worldAxes.get(secondRingAxis(axis).ordinal()));
            axes.add(new AxisVisual(axis, world, directionX, directionY, first.x(), first.y(), second.x(), second.y()));
        }
        return Optional.of(new SceneTransformGizmoState(center, axes, settings.tool()));
    }

    /** Projects a world direction into logical overlay orientation without changing its foreshortening. */
    private Point projectDirection(Vector3f world) {
        projected.set(world);
        camera.get().viewMatrix().transformDirection(projected);
        return new Point(projected.x, -projected.y);
    }

    /** Returns the first basis axis spanning one rotation plane. */
    private static Axis firstRingAxis(Axis axis) {
        return axis == Axis.X ? Axis.Y : Axis.X;
    }

    /** Returns the second basis axis spanning one rotation plane. */
    private static Axis secondRingAxis(Axis axis) {
        return axis == Axis.Z ? Axis.Y : Axis.Z;
    }

    /** Projects one world point into logical viewport coordinates. */
    private @Nullable Point project(Vector3f world, int width, int height) {
        clip.set(world, 1.0f);
        camera.get().viewMatrix().transform(clip);
        camera.get().projectionMatrix().transform(clip);
        if (!Float.isFinite(clip.w) || clip.w <= 0.0f) {
            return null;
        }
        float inverseW = 1.0f / clip.w;
        return new Point((clip.x * inverseW * 0.5f + 0.5f) * width, (0.5f - clip.y * inverseW * 0.5f) * height);
    }
}
