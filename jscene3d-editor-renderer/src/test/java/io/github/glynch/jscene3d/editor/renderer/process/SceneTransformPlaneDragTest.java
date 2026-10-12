/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.cameras.Camera;
import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.AxisVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Handle;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Point;
import java.util.List;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Verifies retained ray-plane intersections for planar translation handles. */
final class SceneTransformPlaneDragTest {
    @Test
    void preservesGrabOffsetAndAvoidsScreenBasisAmplificationInBothProjections() {
        for (Camera camera : List.of(perspective(), orthographic())) {
            SceneTransformPlaneDrag drag = SceneTransformPlaneDrag.begin(
                            camera,
                            nearSingularScreenState(),
                            Handle.XY,
                            new Vector3f(),
                            new Point(400.0f, 300.0f),
                            new RendererSession.SurfaceSize(800, 600))
                    .orElseThrow();

            Vector3f unchanged = drag.delta(400.0f, 300.0f).orElseThrow();
            Vector3f onePixel = drag.delta(400.0f, 301.0f).orElseThrow();

            assertThat(unchanged).isEqualTo(new Vector3f());
            assertThat(onePixel.length()).isLessThan(1.0f);
            assertThat(onePixel.z).isZero();
        }
    }

    @Test
    void rejectsANearlyParallelPointerRayInsteadOfProducingALargeJump() {
        PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 4.0f / 3.0f, 0.01f, 1_000.0f);
        camera.setPosition(10.0f, 0.0f, 0.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);

        assertThat(SceneTransformPlaneDrag.begin(
                        camera,
                        nearSingularScreenState(),
                        Handle.XY,
                        new Vector3f(),
                        new Point(400.0f, 300.0f),
                        new RendererSession.SurfaceSize(800, 600)))
                .isEmpty();
    }

    private static PerspectiveCamera perspective() {
        PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 4.0f / 3.0f, 0.01f, 1_000.0f);
        camera.setPosition(0.0f, 0.0f, 10.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        return camera;
    }

    private static OrthographicCamera orthographic() {
        OrthographicCamera camera = new OrthographicCamera(-5.0f, 5.0f, 5.0f, -5.0f, 0.0f, 1_000.0f);
        camera.setPosition(0.0f, 0.0f, 10.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        return camera;
    }

    private static SceneTransformGizmoState nearSingularScreenState() {
        return new SceneTransformGizmoState(
                new Point(400.0f, 300.0f),
                List.of(
                        axis(Axis.X, new Vector3f(1.0f, 0.0f, 0.0f), 1.0f, 0.0f),
                        axis(Axis.Y, new Vector3f(0.0f, 1.0f, 0.0f), 1.0f, 0.0011f),
                        axis(Axis.Z, new Vector3f(0.0f, 0.0f, 1.0f), 0.0f, 1.0f)),
                SceneTransformTool.MOVE);
    }

    private static AxisVisual axis(Axis axis, Vector3f world, float screenX, float screenY) {
        return new AxisVisual(axis, world, screenX, screenY, 1.0f, 0.0f, 0.0f, 1.0f);
    }
}
