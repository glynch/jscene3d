/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Camera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.OrthographicProjection;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.PerspectiveCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.composition.CompositionScope;
import io.github.glynch.jscene3d.project.entity.EntityId;
import java.math.BigDecimal;
import java.net.URI;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

/** Verifies renderer-owned authored-camera icons, picking, frustums, and lifecycle. */
final class SceneCameraVisualizationsTest {
    private static final AssetId SCENE = AssetId.from("11111111-1111-4111-8111-111111111111");
    private static final EntityId ENTITY = EntityId.from("22222222-2222-4222-8222-222222222222");
    private static final EntityId SECOND_ENTITY = EntityId.from("55555555-5555-4555-8555-555555555555");
    private static final ComponentId PERSPECTIVE = ComponentId.from("33333333-3333-4333-8333-333333333333");
    private static final ComponentId ORTHOGRAPHIC = ComponentId.from("44444444-4444-4444-8444-444444444444");
    private static final ComponentId SECOND_PERSPECTIVE = ComponentId.from("66666666-6666-4666-8666-666666666666");

    @Test
    void presentsOneConstantSizeOccurrenceIconForBothAuthoredCameraVariants() {
        PerspectiveCamera editorCamera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 1_000.0f);
        editorCamera.setPosition(0.0f, 0.0f, 10.0f);
        editorCamera.lookAt(0.0f, 0.0f, 0.0f);
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(content(List.of(perspective(), orthographic())), 1.0f);

            SceneCameraIconState first = visualizations.renderState(800, 600).getFirst();
            SceneCameraIconState resized =
                    visualizations.renderState(1_600, 1_200).getFirst();

            assertThat(first.occurrence()).isEqualTo(occurrence());
            assertThat(first.cameraCount()).isEqualTo(2);
            assertThat(first.hasPerspective()).isTrue();
            assertThat(first.hasOrthographic()).isTrue();
            assertThat(first.bounds().width()).isEqualTo(24.0f);
            assertThat(first.bounds().height()).isEqualTo(20.0f);
            assertThat(first.bounds().width()).isEqualTo(resized.bounds().width());
            assertThat(first.bounds().height()).isEqualTo(resized.bounds().height());
            assertThat(resized.center().x()).isEqualTo(first.center().x() * 2.0f);
            assertThat(resized.center().y()).isEqualTo(first.center().y() * 2.0f);
            assertThat(visualizations.pick(411.9f, 300.0f, 800, 600)).contains(occurrence());
            assertThat(visualizations.pick(412.1f, 300.0f, 800, 600)).isEmpty();
        }
    }

    @Test
    void projectsIconsThroughPerspectiveAndOrthographicEditorCameras() {
        PerspectiveCamera perspectiveEditor = editorCamera();
        OrthographicCamera orthographicEditor = new OrthographicCamera(-5.0f, 5.0f, 5.0f, -5.0f, 0.1f, 100.0f);
        orthographicEditor.setPosition(0.0f, 0.0f, 10.0f);
        orthographicEditor.lookAt(0.0f, 0.0f, 0.0f);
        SceneViewRealizedContent content = content(List.of(perspective()));
        Objects.requireNonNull(content.occurrenceObjects().get(occurrence())).setPosition(2.0f, 1.0f, 0.0f);
        try (SceneCameraVisualizations perspective = new SceneCameraVisualizations(() -> perspectiveEditor, 20.0f);
                SceneCameraVisualizations orthographic =
                        new SceneCameraVisualizations(() -> orthographicEditor, 20.0f)) {
            perspective.replace(content, 1.0f);
            orthographic.replace(content, 1.0f);

            SceneCameraIconState perspectiveState =
                    perspective.renderState(800, 800).getFirst();
            SceneCameraIconState orthographicState =
                    orthographic.renderState(800, 800).getFirst();

            assertThat(perspectiveState.center().x()).isGreaterThan(400.0f);
            assertThat(perspectiveState.center().y()).isLessThan(400.0f);
            assertThat(orthographicState.center().x()).isCloseTo(560.0f, offset(0.001f));
            assertThat(orthographicState.center().y()).isCloseTo(320.0f, offset(0.001f));
        } finally {
            content.close();
        }
    }

    @Test
    void omitsDisabledBehindAndOutsideIconsFromRenderingAndPicking() {
        PerspectiveCamera editorCamera = editorCamera();
        CameraFixture disabled = fixture(occurrence(), ENTITY, false, 0.0f, 0.0f, 0.0f, perspective());
        CameraFixture behind = fixture(secondOccurrence(), SECOND_ENTITY, true, 0.0f, 0.0f, 20.0f, secondPerspective());
        SceneViewRealizedContent content = contentFromFixtures(List.of(disabled, behind));
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(content, 1.0f);

            assertThat(visualizations.renderState(800, 600)).isEmpty();
            assertThat(visualizations.pick(400.0f, 300.0f, 800, 600)).isEmpty();

            behind.object().setPosition(100.0f, 0.0f, 0.0f);
            assertThat(visualizations.renderState(800, 600)).isEmpty();
            assertThat(visualizations.pick(400.0f, 300.0f, 800, 600)).isEmpty();
        } finally {
            content.close();
        }
    }

    @Test
    void omitsAnEnabledCameraBelowAnInvisibleRealizedAncestor() {
        PerspectiveCamera editorCamera = editorCamera();
        SceneViewRealizedContent content = content(List.of(perspective()));
        Object3D occurrenceObject =
                Objects.requireNonNull(content.occurrenceObjects().get(occurrence()));
        Object3D hiddenParent = new Object3D();
        hiddenParent.setVisible(false);
        content.root().add(hiddenParent);
        hiddenParent.add(occurrenceObject);
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(content, 1.0f);

            assertThat(visualizations.renderState(800, 600)).isEmpty();
            assertThat(visualizations.pick(400.0f, 300.0f, 800, 600)).isEmpty();
        } finally {
            content.close();
        }
    }

    @Test
    void resolvesOverlappingIconsBySelectionThenDepthThenStableIdentity() {
        PerspectiveCamera editorCamera = editorCamera();
        CameraFixture far = fixture(occurrence(), ENTITY, true, 0.0f, 0.0f, 0.0f, perspective());
        CameraFixture near = fixture(secondOccurrence(), SECOND_ENTITY, true, 0.0f, 0.0f, 5.0f, secondPerspective());
        SceneViewRealizedContent content = contentFromFixtures(List.of(far, near));
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(content, 1.0f);

            assertThat(visualizations.pick(400.0f, 300.0f, 800, 600)).contains(secondOccurrence());

            visualizations.select(occurrence());
            assertThat(visualizations.pick(400.0f, 300.0f, 800, 600)).contains(occurrence());

            visualizations.select(null);
            near.object().setPosition(0.0f, 0.0f, 0.0f);
            CompositionOccurrenceId expected =
                    occurrence().toString().compareTo(secondOccurrence().toString()) < 0
                            ? occurrence()
                            : secondOccurrence();
            assertThat(visualizations.pick(400.0f, 300.0f, 800, 600)).contains(expected);
        } finally {
            content.close();
        }
    }

    @Test
    void derivesPerspectiveFrustumCornersFromTheEngineProjectionMatrix() {
        PerspectiveCamera3d camera = perspective();
        float aspect = 16.0f / 9.0f;

        SceneCameraFrustumGeometry.Shape shape =
                SceneCameraFrustumGeometry.create(camera, aspect, 400.0f).orElseThrow();
        PerspectiveCamera engineCamera = new PerspectiveCamera(
                (float) Math.toRadians(camera.fieldOfViewDegrees().floatValue()),
                aspect,
                camera.near().floatValue(),
                camera.far().floatValue());

        assertThat(shape.corners()).hasSize(8);
        assertThat(shape.truncated()).isFalse();
        for (int index = 0; index < shape.corners().size(); index++) {
            SceneCameraFrustumGeometry.Corner corner = shape.corners().get(index);
            Vector4f projected = new Vector4f(corner.x(), corner.y(), corner.z(), 1.0f);
            new Matrix4f(engineCamera.projectionMatrix()).transform(projected);
            projected.div(projected.w());
            assertThat(projected.x()).isCloseTo((index & 1) == 0 ? -1.0f : 1.0f, offset(0.000_1f));
            assertThat(projected.y()).isCloseTo((index & 2) == 0 ? -1.0f : 1.0f, offset(0.000_1f));
            assertThat(projected.z()).isCloseTo((index & 4) == 0 ? -1.0f : 1.0f, offset(0.000_1f));
        }
    }

    @Test
    void preservesOrthographicBoundsAndZoomWithoutViewportAspectAdaptation() {
        OrthographicCamera3d camera = orthographic();

        SceneCameraFrustumGeometry.Shape wide =
                SceneCameraFrustumGeometry.create(camera, 2.0f, 400.0f).orElseThrow();
        SceneCameraFrustumGeometry.Shape tall =
                SceneCameraFrustumGeometry.create(camera, 0.5f, 400.0f).orElseThrow();

        assertThat(wide).isEqualTo(tall);
        assertCorner(wide.corners().get(0), -2.0f, -1.5f, -0.1f);
        assertCorner(wide.corners().get(7), 2.0f, 1.5f, -100.0f);
    }

    @Test
    void clampsOnlyDisplayedFarExtentRelativeToTheEditorGridScale() {
        PerspectiveCamera3d camera =
                new PerspectiveCamera3d(identity(PERSPECTIVE), decimal("60"), decimal("0.1"), decimal("5000"), true);

        SceneCameraFrustumGeometry.Shape shape =
                SceneCameraFrustumGeometry.create(camera, 1.0f, 20.0f).orElseThrow();

        assertThat(shape.truncated()).isTrue();
        assertThat(shape.visualizedFar()).isEqualTo(10.0f);
        assertThat(camera.far()).isEqualByComparingTo("5000");
    }

    @Test
    void preservesSmallAuthoredFarDistancesAndTheNearPlaneRelationship() {
        PerspectiveCamera3d shortRange =
                new PerspectiveCamera3d(identity(PERSPECTIVE), decimal("60"), decimal("0.1"), decimal("5"), true);
        PerspectiveCamera3d distantNear =
                new PerspectiveCamera3d(identity(PERSPECTIVE), decimal("60"), decimal("8"), decimal("500"), true);

        SceneCameraFrustumGeometry.Shape shortShape =
                SceneCameraFrustumGeometry.create(shortRange, 1.0f, 20.0f).orElseThrow();
        SceneCameraFrustumGeometry.Shape distantNearShape =
                SceneCameraFrustumGeometry.create(distantNear, 1.0f, 20.0f).orElseThrow();

        assertThat(shortShape.visualizedFar()).isEqualTo(5.0f);
        assertThat(shortShape.truncated()).isFalse();
        assertThat(distantNearShape.visualizedFar()).isEqualTo(16.0f);
        assertThat(distantNearShape.truncated()).isTrue();
    }

    @Test
    void keepsFrustumsSelectedOnlyAndReusesUnchangedResourcesAcrossSnapshotReplacement() {
        PerspectiveCamera editorCamera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 1_000.0f);
        editorCamera.setPosition(0.0f, 0.0f, 10.0f);
        editorCamera.lookAt(0.0f, 0.0f, 0.0f);
        SceneViewRealizedContent firstContent = content(List.of(perspective(), orthographic()));
        SceneViewRealizedContent replacementContent = content(List.of(perspective(), orthographic()));
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(firstContent, 1.0f);
            List<SceneCameraFrustum> first = visualizations.frustumsFor(occurrence());

            assertThat(first).hasSize(2).allSatisfy(frustum -> {
                assertThat(frustum.line().isVisible()).isFalse();
                assertThat(frustum.line().material().transparent()).isTrue();
                assertThat(frustum.line().material().opacity()).isEqualTo(0.62f);
                assertThat(frustum.line().material().depthTestEnabled()).isTrue();
            });
            visualizations.select(occurrence());
            assertThat(first)
                    .allSatisfy(
                            frustum -> assertThat(frustum.line().isVisible()).isTrue());

            visualizations.replace(replacementContent, 1.0f);
            assertThat(visualizations.frustumsFor(occurrence())).containsExactlyElementsOf(first);
            assertThat(first).allSatisfy(frustum -> {
                assertThat(frustum.isClosed()).isFalse();
                assertThat(frustum.line().parent())
                        .isSameAs(replacementContent.occurrenceObjects().get(occurrence()));
            });
        } finally {
            firstContent.close();
            replacementContent.close();
        }
    }

    @Test
    void rebuildsPerspectiveButNotOrthographicFrustumForAChangedViewportAspect() {
        PerspectiveCamera editorCamera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 1_000.0f);
        SceneViewRealizedContent content = content(List.of(perspective(), orthographic()));
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(content, 1.0f);
            List<SceneCameraFrustum> initial = visualizations.frustumsFor(occurrence());

            visualizations.resize(2.0f);
            List<SceneCameraFrustum> resized = visualizations.frustumsFor(occurrence());

            assertThat(resized.get(0)).isNotSameAs(initial.get(0));
            assertThat(initial.get(0).isClosed()).isTrue();
            assertThat(resized.get(1)).isSameAs(initial.get(1));
            assertThat(initial.get(1).isClosed()).isFalse();
        } finally {
            content.close();
        }
    }

    @Test
    void followsTheRealizedHierarchyWithoutApplyingWorldTransformsTwice() {
        PerspectiveCamera editorCamera = editorCamera();
        SceneViewRealizedContent content = content(List.of(perspective()));
        Object3D occurrenceObject =
                Objects.requireNonNull(content.occurrenceObjects().get(occurrence()));
        Object3D root = content.root();
        occurrenceObject.detach();
        Object3D parent = new Object3D();
        Object3D nested = new Object3D();
        root.add(parent);
        parent.add(nested);
        nested.add(occurrenceObject);
        parent.setPosition(3.0f, -2.0f, 1.0f);
        parent.rotateY(0.45f);
        parent.setScale(2.0f, 3.0f, 4.0f);
        nested.setPosition(1.0f, 2.0f, -1.0f);
        nested.rotateX(-0.3f);
        occurrenceObject.setPosition(-0.5f, 0.25f, 0.75f);
        try (SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f)) {
            visualizations.replace(content, 1.0f);
            SceneCameraFrustum frustum =
                    visualizations.frustumsFor(occurrence()).getFirst();

            assertThat(frustum.line().parent()).isSameAs(occurrenceObject);
            Vector4f expected = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
            occurrenceObject.matrixWorld().transform(expected);
            Vector4f actual = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
            frustum.line().matrixWorld().transform(actual);
            assertThat(actual.x()).isCloseTo(expected.x(), offset(0.000_1f));
            assertThat(actual.y()).isCloseTo(expected.y(), offset(0.000_1f));
            assertThat(actual.z()).isCloseTo(expected.z(), offset(0.000_1f));
        } finally {
            content.close();
        }
    }

    @Test
    void replacesChangedAndRemovedResourcesWithoutDisturbingUnchangedCameras() {
        PerspectiveCamera editorCamera = editorCamera();
        SceneViewRealizedContent initialContent = content(List.of(perspective(), orthographic()));
        PerspectiveCamera3d changedPerspective =
                new PerspectiveCamera3d(identity(PERSPECTIVE), decimal("75"), decimal("0.1"), decimal("100"), true);
        SceneViewRealizedContent changedContent = content(List.of(changedPerspective, orthographic()));
        SceneViewRealizedContent removedContent = content(List.of(orthographic()));
        SceneCameraVisualizations visualizations = new SceneCameraVisualizations(() -> editorCamera, 20.0f);
        try {
            visualizations.replace(initialContent, 1.0f);
            List<SceneCameraFrustum> initial = visualizations.frustumsFor(occurrence());

            visualizations.replace(changedContent, 1.0f);
            List<SceneCameraFrustum> changed = visualizations.frustumsFor(occurrence());
            assertThat(initial.get(0).isClosed()).isTrue();
            assertThat(changed.get(0)).isNotSameAs(initial.get(0));
            assertThat(changed.get(1)).isSameAs(initial.get(1));

            visualizations.replace(removedContent, 1.0f);
            assertThat(changed.get(0).isClosed()).isTrue();
            assertThat(visualizations.frustumsFor(occurrence())).containsExactly(changed.get(1));

            visualizations.close();
            assertThat(changed.get(1).isClosed()).isTrue();
            assertThatIllegalStateException()
                    .isThrownBy(() -> visualizations.renderState(800, 600))
                    .withMessage("Scene camera visualizations are closed");
        } finally {
            visualizations.close();
            initialContent.close();
            changedContent.close();
            removedContent.close();
        }
    }

    private static SceneViewRealizedContent content(List<Camera3d> cameras) {
        return contentFromFixtures(List.of(fixture(occurrence(), ENTITY, true, 0.0f, 0.0f, 0.0f, cameras)));
    }

    private static SceneViewRealizedContent contentFromFixtures(List<CameraFixture> fixtures) {
        Object3D root = new Object3D();
        Map<CompositionOccurrenceId, Object3D> objects = new LinkedHashMap<>();
        Map<CompositionOccurrenceId, VisualOccurrence> descriptions = new LinkedHashMap<>();
        fixtures.forEach(fixture -> {
            root.add(fixture.object());
            objects.put(fixture.occurrence(), fixture.object());
            descriptions.put(
                    fixture.occurrence(),
                    new VisualOccurrence(
                            fixture.occurrence(),
                            Optional.empty(),
                            SCENE,
                            URI.create("file:///project/camera.scene.json"),
                            fixture.entity(),
                            Optional.of("Camera"),
                            fixture.enabled(),
                            Optional.empty(),
                            List.of(),
                            Optional.empty(),
                            fixture.cameras()));
        });
        return new SceneViewRealizedContent(
                1L,
                root,
                new LinkedHashMap<>(objects),
                new LinkedHashMap<>(descriptions),
                new IdentityHashMap<>(),
                List.of());
    }

    private static CameraFixture fixture(
            CompositionOccurrenceId occurrence,
            EntityId entity,
            boolean enabled,
            float x,
            float y,
            float z,
            Camera3d... cameras) {
        return fixture(occurrence, entity, enabled, x, y, z, List.of(cameras));
    }

    private static CameraFixture fixture(
            CompositionOccurrenceId occurrence,
            EntityId entity,
            boolean enabled,
            float x,
            float y,
            float z,
            List<Camera3d> cameras) {
        Object3D object = new Object3D();
        object.setPosition(x, y, z);
        return new CameraFixture(occurrence, entity, enabled, object, cameras);
    }

    private static PerspectiveCamera3d perspective() {
        return new PerspectiveCamera3d(identity(PERSPECTIVE), decimal("60"), decimal("0.1"), decimal("100"), true);
    }

    private static PerspectiveCamera3d secondPerspective() {
        return new PerspectiveCamera3d(
                identity(secondOccurrence(), SECOND_ENTITY, SECOND_PERSPECTIVE),
                decimal("60"),
                decimal("0.1"),
                decimal("100"),
                true);
    }

    private static OrthographicCamera3d orthographic() {
        return new OrthographicCamera3d(
                identity(ORTHOGRAPHIC),
                new OrthographicProjection(
                        decimal("-4"),
                        decimal("4"),
                        decimal("3"),
                        decimal("-3"),
                        decimal("0.1"),
                        decimal("100"),
                        decimal("2")),
                false);
    }

    private static ComponentIdentity identity(ComponentId component) {
        return identity(occurrence(), ENTITY, component);
    }

    private static ComponentIdentity identity(
            CompositionOccurrenceId occurrence, EntityId entity, ComponentId component) {
        return new ComponentIdentity(occurrence, new CompositionScope(SCENE, occurrence), entity, component);
    }

    private static CompositionOccurrenceId occurrence() {
        return new CompositionOccurrenceId(SCENE, List.of(ENTITY));
    }

    private static CompositionOccurrenceId secondOccurrence() {
        return new CompositionOccurrenceId(SCENE, List.of(SECOND_ENTITY));
    }

    private static PerspectiveCamera editorCamera() {
        PerspectiveCamera camera = new PerspectiveCamera((float) Math.toRadians(60.0), 1.0f, 0.1f, 1_000.0f);
        camera.setPosition(0.0f, 0.0f, 10.0f);
        camera.lookAt(0.0f, 0.0f, 0.0f);
        return camera;
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static void assertCorner(SceneCameraFrustumGeometry.Corner corner, float x, float y, float z) {
        assertThat(corner.x()).isCloseTo(x, offset(0.000_1f));
        assertThat(corner.y()).isCloseTo(y, offset(0.000_1f));
        assertThat(corner.z()).isCloseTo(z, offset(0.000_1f));
    }

    private record CameraFixture(
            CompositionOccurrenceId occurrence,
            EntityId entity,
            boolean enabled,
            Object3D object,
            List<Camera3d> cameras) {}
}
