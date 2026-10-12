/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Handle;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Point;
import io.github.glynch.jscene3d.raycasting.Raycaster;
import java.util.Objects;
import java.util.Optional;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Stable world-space pointer intersection retained for one planar translation drag. */
final class SceneTransformPlaneDrag {
    private static final float PARALLEL_EPSILON = 1.0e-4f;

    private final Camera camera;
    private final Vector3f planePoint;
    private final Vector3f planeNormal;
    private final Vector3f initialIntersection;
    private final int viewportWidth;
    private final int viewportHeight;

    private SceneTransformPlaneDrag(
            Camera camera,
            Vector3fc planePoint,
            Vector3fc planeNormal,
            Vector3fc initialIntersection,
            int viewportWidth,
            int viewportHeight) {
        this.camera = camera;
        this.planePoint = new Vector3f(planePoint);
        this.planeNormal = new Vector3f(planeNormal);
        this.initialIntersection = new Vector3f(initialIntersection);
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
    }

    /** Creates a plane through the selected origin and captures the initial pointer intersection. */
    static Optional<SceneTransformPlaneDrag> begin(
            Camera camera,
            SceneTransformGizmoState state,
            Handle handle,
            Vector3fc origin,
            Point pointer,
            RendererSession.SurfaceSize viewport) {
        Objects.requireNonNull(camera, "camera");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(viewport, "viewport");
        if (handle.axes().size() != 2) {
            throw new IllegalArgumentException("plane drag requires a two-axis handle");
        }
        Axis first = handle.axes().get(0);
        Axis second = handle.axes().get(1);
        Vector3f normal = state.worldAxis(first).cross(state.worldAxis(second)).normalize();
        return intersect(camera, origin, normal, pointer.x(), pointer.y(), viewport.width(), viewport.height())
                .map(intersection -> new SceneTransformPlaneDrag(
                        camera, origin, normal, intersection, viewport.width(), viewport.height()));
    }

    /** Returns movement from the retained grab point, constrained to the retained world plane. */
    Optional<Vector3f> delta(float horizontal, float vertical) {
        return intersect(camera, planePoint, planeNormal, horizontal, vertical, viewportWidth, viewportHeight)
                .map(intersection -> intersection.sub(initialIntersection));
    }

    private static Optional<Vector3f> intersect(
            Camera camera,
            Vector3fc planePoint,
            Vector3fc planeNormal,
            float horizontal,
            float vertical,
            int viewportWidth,
            int viewportHeight) {
        Raycaster raycaster = new Raycaster();
        float x = horizontal / viewportWidth * 2.0f - 1.0f;
        float y = 1.0f - vertical / viewportHeight * 2.0f;
        raycaster.setFromCamera(x, y, camera);
        Vector3f rayOrigin = raycaster.origin(new Vector3f());
        Vector3f rayDirection = raycaster.direction(new Vector3f());
        float denominator = planeNormal.dot(rayDirection);
        if (!Float.isFinite(denominator) || Math.abs(denominator) < PARALLEL_EPSILON) {
            return Optional.empty();
        }
        float distance = new Vector3f(planePoint).sub(rayOrigin).dot(planeNormal) / denominator;
        Vector3f intersection = rayOrigin.fma(distance, rayDirection);
        return intersection.isFinite() ? Optional.of(intersection) : Optional.empty();
    }
}
