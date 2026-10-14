/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Camera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicProjection;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.PerspectiveCamera3d;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Builds exact camera-local wireframe corners by inverting engine projection matrices. */
final class SceneCameraFrustumGeometry {
    private static final float FAR_EXTENT_GRID_MULTIPLIER = 50.0f;
    private static final float MINIMUM_FAR_TO_NEAR_RATIO = 2.0f;
    private static final int[] EDGE_INDICES = {
        0, 1, 1, 3, 3, 2, 2, 0,
        4, 5, 5, 7, 7, 6, 6, 4,
        0, 4, 1, 5, 2, 6, 3, 7
    };

    private SceneCameraFrustumGeometry() {
        throw new AssertionError("SceneCameraFrustumGeometry cannot be instantiated");
    }

    /**
     * Creates camera-local frustum corners using the engine projection implementation.
     *
     * <p>The displayed far extent is limited to fifty times the editor grid size, or twice the
     * authored near plane when that is larger. This keeps extreme authored clipping distances
     * useful in Scene View without changing the retained authored values or runtime projection.
     */
    static Optional<Shape> create(Camera3d description, float aspectRatio, float sceneScale) {
        if (!Float.isFinite(aspectRatio) || aspectRatio <= 0.0f) {
            throw new IllegalArgumentException("aspectRatio must be finite and positive");
        }
        if (!Float.isFinite(sceneScale) || sceneScale <= 0.0f) {
            throw new IllegalArgumentException("sceneScale must be finite and positive");
        }
        try {
            float near = finite(description.near().floatValue());
            float authoredFar = finite(description.far().floatValue());
            float limit = Math.max(sceneScale * FAR_EXTENT_GRID_MULTIPLIER, near * MINIMUM_FAR_TO_NEAR_RATIO);
            float visualizedFar = Math.min(authoredFar, limit);
            Camera camera = createCamera(description, aspectRatio, near, visualizedFar);
            return Optional.of(new Shape(
                    unprojectCorners(camera.inverseProjectionMatrix()), visualizedFar, visualizedFar < authoredFar));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    /** Creates indexed line-segment geometry for all twelve frustum edges. */
    static BufferGeometry geometry(Shape shape) {
        float[] positions = new float[shape.corners().size() * 3];
        for (int index = 0; index < shape.corners().size(); index++) {
            Corner corner = shape.corners().get(index);
            int offset = index * 3;
            positions[offset] = corner.x();
            positions[offset + 1] = corner.y();
            positions[offset + 2] = corner.z();
        }
        return BufferGeometry.builder()
                .positions(positions)
                .indices(EDGE_INDICES)
                .build();
    }

    /** Configures the appropriate engine camera with authored semantics and displayed far extent. */
    private static Camera createCamera(Camera3d description, float aspectRatio, float near, float far) {
        return switch (description) {
            case PerspectiveCamera3d perspective ->
                new PerspectiveCamera(
                        (float) Math.toRadians(
                                finite(perspective.fieldOfViewDegrees().floatValue())),
                        aspectRatio,
                        near,
                        far);
            case OrthographicCamera3d orthographic -> orthographicCamera(orthographic.projection(), near, far);
        };
    }

    /** Configures exact authored orthographic bounds and zoom without aspect adaptation. */
    private static OrthographicCamera orthographicCamera(OrthographicProjection projection, float near, float far) {
        OrthographicCamera camera = new OrthographicCamera(
                finite(projection.left().floatValue()),
                finite(projection.right().floatValue()),
                finite(projection.top().floatValue()),
                finite(projection.bottom().floatValue()),
                near,
                far);
        camera.setZoom(finite(projection.zoom().floatValue()));
        return camera;
    }

    /** Unprojects the eight OpenGL NDC corners in stable near-then-far bit order. */
    private static List<Corner> unprojectCorners(Matrix4fc inverseProjection) {
        List<Corner> corners = new ArrayList<>(8);
        Vector4f point = new Vector4f();
        for (int index = 0; index < 8; index++) {
            point.set(
                    (index & 1) == 0 ? -1.0f : 1.0f,
                    (index & 2) == 0 ? -1.0f : 1.0f,
                    (index & 4) == 0 ? -1.0f : 1.0f,
                    1.0f);
            inverseProjection.transform(point);
            if (!point.isFinite() || point.w() == 0.0f) {
                throw new IllegalArgumentException("projection inverse produced an invalid frustum corner");
            }
            point.div(point.w());
            corners.add(new Corner(finite(point.x()), finite(point.y()), finite(point.z())));
        }
        return List.copyOf(corners);
    }

    /** Requires a finite float converted from authored exact decimal state. */
    private static float finite(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("camera projection value exceeds renderer precision");
        }
        return value;
    }

    /** Immutable local-space frustum geometry plus visualization-clamp metadata. */
    record Shape(List<Corner> corners, float visualizedFar, boolean truncated) {
        /** Copies and validates shape state. */
        Shape {
            corners = List.copyOf(corners);
            if (corners.size() != 8 || !Float.isFinite(visualizedFar) || visualizedFar <= 0.0f) {
                throw new IllegalArgumentException("frustum shape is invalid");
            }
        }
    }

    /** One finite camera-local frustum corner. */
    record Corner(float x, float y, float z) {
        /** Validates all coordinates. */
        Corner {
            finite(x);
            finite(y);
            finite(z);
        }
    }
}
