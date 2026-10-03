/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.AxisVisual;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Backdrop;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.Direction;
import io.github.glynch.jscene3d.editor.renderer.process.SceneOrientationGizmoState.TipStyle;
import io.github.glynch.jscene3d.math.Color;
import java.util.List;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

/** Verifies semantic, screen-space state for the editor-owned Scene orientation gizmo. */
final class SceneOrientationGizmoTest {
    private static final float EPSILON = 0.000_001f;

    @Test
    void describesCubeSignedAxesConventionalColorsAndLabelsWithoutAStatusCard() {
        PerspectiveCamera camera = camera();
        try (SceneOrientationGizmo gizmo = new SceneOrientationGizmo(camera)) {
            SceneOrientationGizmoState state = gizmo.renderState(1_200, 800);

            assertThat(positive(state, Axis.X)).satisfies(axis -> {
                assertThat(axis.color()).isEqualTo(Color.RED);
                assertThat(axis.label()).contains("X");
            });
            assertThat(positive(state, Axis.Y)).satisfies(axis -> {
                assertThat(axis.color()).isEqualTo(Color.GREEN);
                assertThat(axis.label()).contains("Y");
            });
            assertThat(positive(state, Axis.Z)).satisfies(axis -> {
                assertThat(axis.color()).isEqualTo(Color.BLUE);
                assertThat(axis.label()).contains("Z");
            });
            assertThat(state.axes())
                    .extracting(AxisVisual::direction)
                    .containsOnly(Direction.NEGATIVE, Direction.POSITIVE)
                    .hasSize(6);
            assertThat(state.axes())
                    .filteredOn(axis -> axis.direction() == Direction.NEGATIVE)
                    .allSatisfy(axis -> {
                        assertThat(axis.label()).isEmpty();
                        assertThat(axis.alpha())
                                .isLessThan(positive(state, axis.axis()).alpha());
                    });
            assertThat(state.cube().faces()).isNotEmpty();
            assertThat(state.cube().hitBounds().width()).isPositive();
            assertThat(state.cube().hitBounds().height()).isPositive();
            assertThat(state.backdrop()).isEqualTo(Backdrop.NONE);
        }
    }

    @Test
    void retainsArrowAndHitTestGeometryForFutureInteraction() {
        try (SceneOrientationGizmo gizmo = new SceneOrientationGizmo(camera())) {
            SceneOrientationGizmoState state = gizmo.renderState(1_200, 800);

            assertThat(state.axes()).allSatisfy(axis -> {
                assertThat(axis.hitBounds().width()).isPositive();
                assertThat(axis.hitBounds().height()).isPositive();
                if (axis.tipStyle() == TipStyle.ARROW) {
                    assertThat(axis.head()).hasSize(3);
                    assertThat(axis.stemEnd()).isNotEqualTo(axis.endpoint());
                } else {
                    assertThat(axis.tipStyle()).isEqualTo(TipStyle.END_ON);
                    assertThat(axis.head()).isEmpty();
                }
            });
        }
    }

    @Test
    void remainsTopRightAndConstantSizeAcrossViewportResize() {
        try (SceneOrientationGizmo gizmo = new SceneOrientationGizmo(camera())) {
            SceneOrientationGizmoState first = gizmo.renderState(800, 600);
            SceneOrientationGizmoState resized = gizmo.renderState(1_600, 1_000);

            assertThat(first.bounds().width()).isEqualTo(SceneOrientationGizmo.WIDTH);
            assertThat(first.bounds().height()).isEqualTo(SceneOrientationGizmo.HEIGHT);
            assertThat(resized.bounds().width()).isEqualTo(first.bounds().width());
            assertThat(resized.bounds().height()).isEqualTo(first.bounds().height());
            assertThat(first.bounds().y()).isEqualTo(SceneOrientationGizmo.MARGIN);
            assertThat(resized.bounds().y()).isEqualTo(SceneOrientationGizmo.MARGIN);
            assertThat(800.0f - first.bounds().x() - first.bounds().width()).isEqualTo(SceneOrientationGizmo.MARGIN);
            assertThat(1_600.0f - resized.bounds().x() - resized.bounds().width())
                    .isEqualTo(SceneOrientationGizmo.MARGIN);
        }
    }

    @Test
    void projectsAxesAndCentralCubeThroughCurrentEditorCameraOrientation() {
        PerspectiveCamera camera = camera();
        try (SceneOrientationGizmo gizmo = new SceneOrientationGizmo(camera)) {
            SceneOrientationGizmoState initial = gizmo.renderState(1_200, 800);
            AxisVisual initialX = positive(initial, Axis.X);
            List<Axis> initialFaces = initial.cube().faces().stream()
                    .map(SceneOrientationGizmoState.CubeFaceVisual::axis)
                    .toList();

            camera.setQuaternion(new Quaternionf().rotationY((float) (Math.PI * 0.5)));
            SceneOrientationGizmoState rotated = gizmo.renderState(1_200, 800);
            AxisVisual rotatedX = positive(rotated, Axis.X);
            AxisVisual rotatedZ = positive(rotated, Axis.Z);

            assertThat(initialX.endpoint().x()).isGreaterThan(initial.center().x());
            assertThat(rotatedX.endpoint().x()).isCloseTo(rotated.center().x(), offset(EPSILON));
            assertThat(rotatedZ.endpoint().x()).isNotCloseTo(rotated.center().x(), offset(EPSILON));
            assertThat(rotated.cube().faces().stream()
                            .map(SceneOrientationGizmoState.CubeFaceVisual::axis)
                            .toList())
                    .isNotEqualTo(initialFaces);
        }
    }

    @Test
    void retainsBackToFrontDepthOrdering() {
        PerspectiveCamera camera = camera();
        camera.setQuaternion(new Quaternionf().rotationXYZ(0.4f, -0.65f, 0.2f));
        try (SceneOrientationGizmo gizmo = new SceneOrientationGizmo(camera)) {
            SceneOrientationGizmoState state = gizmo.renderState(1_200, 800);

            assertThat(state.axes())
                    .isSortedAccordingTo((first, second) -> Float.compare(first.depth(), second.depth()));
            assertThat(state.cube().faces())
                    .isSortedAccordingTo((first, second) -> Float.compare(first.depth(), second.depth()));
        }
    }

    @Test
    void closesIdempotentlyWithItsOwningSessionLifecycle() {
        SceneOrientationGizmo gizmo = new SceneOrientationGizmo(camera());

        gizmo.close();
        gizmo.close();

        assertThat(gizmo.isClosed()).isTrue();
        assertThatIllegalStateException()
                .isThrownBy(() -> gizmo.renderState(800, 600))
                .withMessage("Scene orientation gizmo is closed");
    }

    /** Returns the positive visual for one axis. */
    private static AxisVisual positive(SceneOrientationGizmoState state, Axis axis) {
        List<AxisVisual> matches = state.axes().stream()
                .filter(visual -> visual.axis() == axis && visual.direction() == Direction.POSITIVE)
                .toList();
        assertThat(matches).hasSize(1);
        return matches.getFirst();
    }

    /** Creates a perspective editor camera with identity orientation. */
    private static PerspectiveCamera camera() {
        return new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 1_000.0f);
    }
}
