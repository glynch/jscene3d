/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.cameras.OrthographicCamera;
import io.github.glynch.jscene3d.cameras.PerspectiveCamera;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformEvent.Phase;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmo.PointerPhase;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Axis;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Handle;
import io.github.glynch.jscene3d.editor.renderer.process.SceneTransformGizmoState.Point;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Code;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Status;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.DirectionalLight3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.MeshRenderer3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.PerspectiveCamera3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.helpers.BoxHelper;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
import io.github.glynch.jscene3d.math.Color;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.objects.Object3D;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.composition.CompositionOccurrenceId;
import io.github.glynch.jscene3d.project.composition.CompositionScope;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ResourceReference;
import io.github.glynch.jscene3d.textures.Texture;
import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Verifies safe, revision-aware realization of Scene View snapshots. */
final class SceneViewRendererSessionTest {
    private static final AssetId SCENE = AssetId.from("11111111-1111-4111-8111-111111111111");
    private static final AssetId DEFINITION = AssetId.from("22222222-2222-4222-8222-222222222222");
    private static final EntityId ROOT_ENTITY = EntityId.from("33333333-3333-4333-8333-333333333333");
    private static final EntityId CHILD_ENTITY = EntityId.from("44444444-4444-4444-8444-444444444444");
    private static final EntityId SECOND_CHILD_ENTITY = EntityId.from("55555555-5555-4555-8555-555555555555");
    private static final ComponentId TRANSFORM = ComponentId.from("66666666-6666-4666-8666-666666666666");
    private static final ComponentId MESH = ComponentId.from("77777777-7777-4777-8777-777777777777");
    private static final ComponentId SECOND_MESH = ComponentId.from("99999999-9999-4999-8999-999999999991");
    private static final ComponentId THIRD_MESH = ComponentId.from("99999999-9999-4999-8999-999999999992");
    private static final ComponentId LIGHT = ComponentId.from("88888888-8888-4888-8888-888888888888");
    private static final ComponentId AUTHORED_CAMERA = ComponentId.from("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final ResourceReference MESH_REFERENCE = ResourceReference.asset("box-mesh");
    private static final ResourceReference MATERIAL_REFERENCE = ResourceReference.asset("textured-material");
    private static final ResourceReference UNSUPPORTED_MATERIAL_REFERENCE = ResourceReference.asset("unsupported");
    private static final URI SOURCE = URI.create("file:///project/scenes/example.scene.json");

    @Test
    void realizesEmptySnapshotsBesideEditorOwnedCameraGridAndOverlayGizmo() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            SceneViewRealizationResult result = session.replaceSnapshot(snapshot(0L, List.of()));

            assertThat(result.status()).isEqualTo(Status.APPLIED);
            assertThat(result.currentRevision()).hasValue(0L);
            assertThat(session.scene().children())
                    .contains(session.grid())
                    .doesNotContain(session.camera())
                    .hasSize(2);
            assertThat(session.orientationGizmo()).isNotInstanceOf(Object3D.class);
            assertThat(session.identityFor(session.camera())).isEmpty();
            assertThat(session.identityFor(session.grid())).isEmpty();
            assertThat(resolver.acquisitionCount()).isZero();
        }
    }

    @Test
    void retainsAuthoredCameraSemanticsAcrossSnapshotReplacementAndRemoval() {
        PerspectiveCamera3d first = new PerspectiveCamera3d(
                component(rootOccurrence(), AUTHORED_CAMERA), decimal("55"), decimal("0.1"), decimal("500"), true);
        PerspectiveCamera3d replacement = new PerspectiveCamera3d(
                component(rootOccurrence(), AUTHORED_CAMERA), decimal("65"), decimal("0.2"), decimal("750"), false);
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(authoredCameraRoot(first))));
            assertThat(session.camerasFor(rootOccurrence()).getFirst()).isSameAs(first);

            session.replaceSnapshot(snapshot(2L, List.of(authoredCameraRoot(replacement))));
            assertThat(session.camerasFor(rootOccurrence()).getFirst()).isSameAs(replacement);

            session.replaceSnapshot(snapshot(3L, List.of()));
            assertThat(session.camerasFor(rootOccurrence())).isEmpty();
        }
    }

    @Test
    void selectsCameraOnlyOccurrenceThroughItsOverlayIconAndShowsAllFrustums() {
        PerspectiveCamera3d perspective = new PerspectiveCamera3d(
                component(rootOccurrence(), AUTHORED_CAMERA), decimal("55"), decimal("0.1"), decimal("500"), true);
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.updateViewportSize(1_000, 700);
            session.replaceSnapshot(snapshot(1L, List.of(authoredCameraRoot(perspective))));

            SceneViewSelectionResult selected = session.pick(1L, 0.0f, 0.0f);

            assertThat(selected).isEqualTo(SceneViewSelectionResult.selected(1L, rootOccurrence()));
            assertThat(session.cameraVisualizations()
                            .renderState(1_000, 700)
                            .getFirst()
                            .selected())
                    .isTrue();
            assertThat(session.cameraVisualizations().frustumsFor(rootOccurrence()))
                    .hasSize(1)
                    .allSatisfy(
                            frustum -> assertThat(frustum.line().isVisible()).isTrue());
        }
    }

    @Test
    void focusSelectedFramesCameraOnlyOccurrenceWithoutUsingItsFrustumExtent() {
        PerspectiveCamera3d perspective = new PerspectiveCamera3d(
                component(rootOccurrence(), AUTHORED_CAMERA), decimal("55"), decimal("0.1"), decimal("500"), true);
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(authoredCameraRoot(perspective))));
            session.select(1L, Optional.of(rootOccurrence()));

            session.navigate(new SceneViewNavigation.FocusSelected());

            assertThat(cameraDistance(session.cameraState())).isLessThan(5.0f);
        }
    }

    @Test
    void updatesEditorCameraAspectFromTheCurrentPhysicalViewport() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.updateViewportSize(1_920, 1_080);
            assertThat(((PerspectiveCamera) session.camera()).aspectRatio())
                    .isCloseTo(16.0f / 9.0f, offset(0.000_001f));

            session.updateViewportSize(900, 1_600);
            assertThat(((PerspectiveCamera) session.camera()).aspectRatio())
                    .isCloseTo(9.0f / 16.0f, offset(0.000_001f));
        }
    }

    @Test
    void navigatesOneSessionLocalEditorCameraWithoutChangingSceneContent() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(pickableRoot())));
            Object3D authored = session.objectFor(rootOccurrence()).orElseThrow();
            SceneViewCameraState initial = session.cameraState();

            session.navigate(new SceneViewNavigation.Orbit(80.0f, -30.0f, 1_200, 800));
            SceneViewCameraState orbited = session.cameraState();
            session.navigate(new SceneViewNavigation.Pan(24.0f, -18.0f, 1_200, 800));
            SceneViewCameraState panned = session.cameraState();
            session.navigate(new SceneViewNavigation.Zoom(-120.0f));
            SceneViewCameraState zoomed = session.cameraState();

            assertThat(orbited.position()).isNotEqualTo(initial.position());
            assertThat(orbited.pivot()).isEqualTo(initial.pivot());
            assertThat(panned.pivot()).isNotEqualTo(orbited.pivot());
            assertThat(cameraDistance(zoomed)).isLessThan(cameraDistance(panned));
            assertThat(orbited.orientationDegrees()).isNotEqualTo(initial.orientationDegrees());
            assertThat(session.objectFor(rootOccurrence())).containsSame(authored);
            assertThat(session.revision()).hasValue(1L);
        }
    }

    @Test
    void framesSelectionAndSceneAndTogglesProjectionThroughGizmoCentre() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(pickableRoot())));
            session.select(1L, Optional.of(rootOccurrence()));

            session.navigate(new SceneViewNavigation.FocusSelected());
            SceneViewCameraState focused = session.cameraState();
            assertThat(focused.pivot()).isEqualTo(new Vector3f());
            assertThat(cameraDistance(focused)).isLessThan(3.0f);

            session.navigate(new SceneViewNavigation.Pan(50.0f, 0.0f, 1_000, 700));
            session.navigate(new SceneViewNavigation.FrameAll());
            assertThat(session.cameraState().pivot()).isEqualTo(new Vector3f());

            SceneOrientationGizmoState gizmo = session.orientationGizmo().renderState(1_000, 700);
            session.navigate(new SceneViewNavigation.GizmoClick(
                    gizmo.center().x(), gizmo.center().y(), 1_000, 700));
            assertThat(session.camera()).isInstanceOf(OrthographicCamera.class);
            assertThat(session.cameraState().projection()).isEqualTo(SceneViewCameraState.Projection.ORTHOGRAPHIC);
            assertThat(session.orientationGizmo().renderState(1_000, 700).projectionLabel())
                    .isEqualTo("Ortho");
        }
    }

    @Test
    void clickingGizmoAxisAlignsCameraAndSnapshotReplacementRetainsItsState() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(pickableRoot())));
            SceneOrientationGizmoState state = session.orientationGizmo().renderState(1_000, 700);
            SceneOrientationGizmoState.AxisVisual positiveX = state.axes().stream()
                    .filter(axis -> axis.axis() == SceneOrientationGizmoState.Axis.X
                            && axis.direction() == SceneOrientationGizmoState.Direction.POSITIVE)
                    .findFirst()
                    .orElseThrow();

            session.navigate(new SceneViewNavigation.GizmoClick(
                    positiveX.endpoint().x(), positiveX.endpoint().y(), 1_000, 700));
            SceneViewCameraState aligned = session.cameraState();
            session.replaceSnapshot(snapshot(2L, List.of(pickableRoot())));

            assertThat(aligned.position().x()).isGreaterThan(aligned.pivot().x());
            assertThat(aligned.position().y()).isCloseTo(aligned.pivot().y(), offset(0.000_1f));
            assertThat(aligned.position().z()).isCloseTo(aligned.pivot().z(), offset(0.000_1f));
            assertThat(session.cameraState()).isEqualTo(aligned);
        }
    }

    @Test
    void realizesHierarchyTransformsAndDistinctRepeatedOccurrences() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            session.replaceSnapshot(snapshot(1L, visualOccurrences()));
            Object3D root = session.objectFor(rootOccurrence()).orElseThrow();
            Object3D first = session.objectFor(childOccurrence()).orElseThrow();
            Object3D second = session.objectFor(secondChildOccurrence()).orElseThrow();

            assertThat(first.parent()).isSameAs(root);
            assertThat(second.parent()).isSameAs(root);
            assertThat(first).isNotSameAs(second);
            assertThat(first.position()).isEqualTo(new Vector3f(1.0f, 2.0f, 3.0f));
            assertThat(first.scale()).isEqualTo(new Vector3f(2.0f, 3.0f, 4.0f));
            assertThat(first.isVisible()).isFalse();
            assertThat(second.isVisible()).isTrue();
            assertRotation(first);
        }
    }

    @Test
    void realizesTexturedMeshAndDirectionalLightWithReverseIdentity() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            session.replaceSnapshot(snapshot(1L, visualOccurrences()));
            Object3D occurrence = session.objectFor(childOccurrence()).orElseThrow();
            Mesh mesh = (Mesh) occurrence.children().get(0);
            DirectionalLight light = (DirectionalLight) occurrence.children().get(1);

            assertThat(mesh.geometry()).isSameAs(resolver.geometry());
            assertThat(mesh.material()).isSameAs(resolver.material());
            assertThat(((BasicMaterial) mesh.material()).colorMap()).contains(resolver.texture());
            assertThat(mesh.isVisible()).isFalse();
            assertThat(light.color().red()).isEqualTo(0.25f);
            assertThat(light.color().green()).isEqualTo(0.5f);
            assertThat(light.color().blue()).isEqualTo(0.75f);
            assertThat(light.intensity()).isEqualTo(3.5f);
            assertThat(light.target(new Vector3f())).isEqualTo(new Vector3f(7.0f, 8.0f, 9.0f));
            assertThat(session.identityFor(mesh))
                    .contains(SceneViewObjectIdentity.component(component(childOccurrence(), MESH)));
            assertThat(session.identityFor(light))
                    .contains(SceneViewObjectIdentity.component(component(childOccurrence(), LIGHT)));
        }
    }

    @Test
    void keepsEditorCameraGridAndOrientationGizmoIndependentAcrossReplacement() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            session.replaceSnapshot(snapshot(1L, visualOccurrences()));
            Object3D oldOccurrence = session.objectFor(childOccurrence()).orElseThrow();
            Object3D camera = session.camera();
            Object3D grid = session.grid();
            SceneOrientationGizmo orientationGizmo = session.orientationGizmo();

            SceneViewRealizationResult result = session.replaceSnapshot(snapshot(2L, visualOccurrences()));

            assertThat(result.status()).isEqualTo(Status.APPLIED);
            assertThat(session.camera()).isSameAs(camera);
            assertThat(session.grid()).isSameAs(grid);
            assertThat(session.orientationGizmo()).isSameAs(orientationGizmo);
            assertThat(orientationGizmo).isNotInstanceOf(Object3D.class);
            assertThat(session.objectFor(childOccurrence())).hasValueSatisfying(current -> {
                assertThat(current).isNotSameAs(oldOccurrence);
                assertThat(current.parent()).isNotNull();
            });
            Object3D oldAuthoredRoot = Objects.requireNonNull(oldOccurrence.parent());
            Object3D oldContentRoot = Objects.requireNonNull(oldAuthoredRoot.parent());
            assertThat(oldContentRoot.parent()).isNull();
        }
    }

    @Test
    void disposesOrientationGizmoWithTheRendererSession() {
        SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver());
        SceneOrientationGizmo orientationGizmo = session.orientationGizmo();

        session.close();
        session.close();

        assertThat(orientationGizmo.isClosed()).isTrue();
    }

    @Test
    void exposesConstantScreenSpaceMoveAxesAndPlanePicking() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(transformableRoot())));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));

            SceneTransformGizmoState small =
                    session.transformGizmo().renderState(800, 600).orElseThrow();
            SceneTransformGizmoState large =
                    session.transformGizmo().renderState(1_600, 1_200).orElseThrow();
            Point xSmall = small.endpoint(Axis.X);
            Point xLarge = large.endpoint(Axis.X);
            Point plane = small.planeCenter(Handle.XY);

            assertThat(distance(small.center(), xSmall)).isCloseTo(132.0f, offset(0.001f));
            assertThat(distance(large.center(), xLarge)).isCloseTo(132.0f, offset(0.001f));
            assertThat(small.hit(xSmall.x(), xSmall.y())).contains(Handle.X);
            assertThat(small.hit(plane.x(), plane.y())).contains(Handle.XY);

            SceneTransformGizmoState.AxisVisual xAxis = small.axis(Axis.X);
            Point tolerantAxisHit = new Point(
                    small.center().x() + xAxis.screenX() * 96.0f - xAxis.screenY() * 15.0f,
                    small.center().y() + xAxis.screenY() * 96.0f + xAxis.screenX() * 15.0f);
            assertThat(small.hit(tolerantAxisHit.x(), tolerantAxisHit.y())).contains(Handle.X);
            for (Handle handle : List.of(Handle.XY, Handle.XZ, Handle.YZ)) {
                Point planeCenter = small.planeCenter(handle);
                assertThat(small.hit(planeCenter.x() + 15.0f, planeCenter.y())).contains(handle);
            }
        }
    }

    @Test
    void exposesTolerantRotationAndScaleHandlesWithDistinctInteractionColors() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(transformableRoot())));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.ROTATE, SceneTransformSpace.GLOBAL, false));
            SceneTransformGizmoState rotation =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point ring = rotation.ringPoint(Axis.X, 0.0f);
            float ringDistance = distance(rotation.center(), ring);
            Point tolerantRingHit = new Point(
                    ring.x() + (ring.x() - rotation.center().x()) / ringDistance * 15.0f,
                    ring.y() + (ring.y() - rotation.center().y()) / ringDistance * 15.0f);

            assertThat(rotation.hit(tolerantRingHit.x(), tolerantRingHit.y())).contains(Handle.X);

            session.configureTransform(settings(SceneTransformTool.SCALE, SceneTransformSpace.GLOBAL, false));
            SceneTransformGizmoState scale =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            assertThat(scale.hit(scale.center().x() + 15.0f, scale.center().y()))
                    .contains(Handle.UNIFORM);

            Color normal = SceneTransformGizmoRenderer.color(Axis.X, Handle.X, null, null, true);
            Color hovered = SceneTransformGizmoRenderer.color(Axis.X, Handle.X, Handle.X, null, true);
            Color active = SceneTransformGizmoRenderer.color(Axis.X, Handle.X, Handle.X, Handle.X, true);
            assertThat(normal).isEqualTo(Color.srgb(0xf04b52));
            assertThat(hovered).isEqualTo(Color.srgb(0xffe45c));
            assertThat(active).isEqualTo(Color.srgb(0xfff29a)).isNotEqualTo(hovered);
        }
    }

    @Test
    void completesOneDragTransactionForEveryMoveRotateAndScaleHandle() {
        assertHandleTransactions(
                SceneTransformTool.MOVE,
                "position",
                List.of(Handle.X, Handle.Y, Handle.Z, Handle.XY, Handle.XZ, Handle.YZ));
        assertHandleTransactions(SceneTransformTool.ROTATE, "orientation", List.of(Handle.X, Handle.Y, Handle.Z));
        assertHandleTransactions(
                SceneTransformTool.SCALE, "scale", List.of(Handle.X, Handle.Y, Handle.Z, Handle.UNIFORM));
    }

    @Test
    void exposesTransformGizmoInOrthographicProjection() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            Transform3d transform = new Transform3d(
                    authoredComponent(rootOccurrence(), TRANSFORM),
                    vector("-1.5", "0", "0"),
                    vector("0", "0", "0"),
                    vector("1", "1", "1"));
            VisualOccurrence occurrence = occurrence(
                    rootOccurrence(),
                    Optional.empty(),
                    ROOT_ENTITY,
                    true,
                    Optional.of(transform),
                    List.of(),
                    Optional.empty());
            session.replaceSnapshot(snapshot(1L, List.of(occurrence)));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            session.updateViewportSize(1_000, 700);

            session.navigate(new SceneViewNavigation.ToggleProjection());

            assertThat(session.camera()).isInstanceOf(OrthographicCamera.class);
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            assertThat(state.center().x()).isBetween(0.0f, 1_000.0f);
            assertThat(state.center().y()).isBetween(0.0f, 700.0f);
        }
    }

    @Test
    void projectionSwitchPreservesSelectedOriginAndGizmoAlignment() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            Transform3d transform = new Transform3d(
                    authoredComponent(rootOccurrence(), TRANSFORM),
                    vector("2.5", "-1.25", "0.75"),
                    vector("0", "0", "0"),
                    vector("1", "1", "1"));
            VisualOccurrence occurrence = occurrence(
                    rootOccurrence(),
                    Optional.empty(),
                    ROOT_ENTITY,
                    true,
                    Optional.of(transform),
                    List.of(),
                    Optional.empty());
            session.replaceSnapshot(snapshot(1L, List.of(occurrence)));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            session.updateViewportSize(1_000, 700);
            SceneTransformGizmoState perspective =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();

            session.navigate(new SceneViewNavigation.ToggleProjection());
            SceneTransformGizmoState orthographic =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();

            assertThat(session.objectFor(rootOccurrence()).orElseThrow().position())
                    .isEqualTo(new Vector3f(2.5f, -1.25f, 0.75f));
            assertThat(orthographic.center().x()).isBetween(0.0f, 1_000.0f);
            assertThat(orthographic.center().y()).isBetween(0.0f, 700.0f);
            assertThat(distance(perspective.center(), orthographic.center())).isLessThan(120.0f);
        }
    }

    @Test
    void planeMovesStayBoundedAndConstrainedInPerspectiveAndOrthographicProjection() {
        for (boolean orthographic : List.of(false, true)) {
            for (Handle handle : List.of(Handle.XY, Handle.XZ, Handle.YZ)) {
                try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
                    session.replaceSnapshot(snapshot(1L, List.of(transformableRoot())));
                    session.select(1L, Optional.of(rootOccurrence()));
                    session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
                    session.updateViewportSize(1_000, 700);
                    if (orthographic) {
                        session.navigate(new SceneViewNavigation.ToggleProjection());
                    }
                    SceneTransformGizmoState state =
                            session.transformGizmo().renderState(1_000, 700).orElseThrow();
                    Point start = findHandle(state, handle);

                    assertThat(session.transform(PointerPhase.BEGIN, start.x(), start.y(), 1_000, 700)
                                    .event())
                            .hasValueSatisfying(
                                    begin -> assertThat(begin.phase()).isEqualTo(Phase.BEGIN));
                    SceneTransformEvent unchanged = session.transform(
                                    PointerPhase.UPDATE, start.x(), start.y(), 1_000, 700)
                            .event()
                            .orElseThrow();
                    SceneTransformEvent moved = session.transform(
                                    PointerPhase.UPDATE, start.x() + 24.0f, start.y() - 18.0f, 1_000, 700)
                            .event()
                            .orElseThrow();

                    assertThat(unchanged.value().x()).isEqualByComparingTo("0");
                    assertThat(unchanged.value().y()).isEqualByComparingTo("0");
                    assertThat(unchanged.value().z()).isEqualByComparingTo("0");
                    assertExcludedPlaneCoordinate(handle, moved.value());
                    assertThat(vectorLength(moved.value())).isLessThan(10.0f);
                }
            }
        }
    }

    @Test
    void localPlaneMoveAppliesTranslationSnapping() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(rotatedTransformableRoot())));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.LOCAL, true));
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point start = findHandle(state, Handle.XZ);

            assertThat(session.transform(PointerPhase.BEGIN, start.x(), start.y(), 1_000, 700)
                            .event())
                    .hasValueSatisfying(begin -> assertThat(begin.phase()).isEqualTo(Phase.BEGIN));
            SceneTransformEvent moved = session.transform(
                            PointerPhase.UPDATE, start.x() + 31.0f, start.y() - 19.0f, 1_000, 700)
                    .event()
                    .orElseThrow();

            for (BigDecimal coordinate :
                    List.of(moved.value().x(), moved.value().y(), moved.value().z())) {
                assertThat(coordinate.remainder(new BigDecimal("0.5"))).isEqualByComparingTo("0");
            }
        }
    }

    @Test
    void emitsOneMoveDragLifecycleAndRestoresPreviewOnCancel() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(5L, List.of(transformableRoot())));
            session.select(5L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point endpoint = state.endpoint(Axis.X);

            SceneTransformEvent begin = session.transform(PointerPhase.BEGIN, endpoint.x(), endpoint.y(), 1_000, 700)
                    .event()
                    .orElseThrow();
            SceneTransformEvent update = session.transform(
                            PointerPhase.UPDATE, endpoint.x() + 41.0f, endpoint.y(), 1_000, 700)
                    .event()
                    .orElseThrow();
            Object3D selected = session.objectFor(rootOccurrence()).orElseThrow();

            assertThat(begin.phase()).isEqualTo(Phase.BEGIN);
            assertThat(update.phase()).isEqualTo(Phase.UPDATE);
            assertThat(update.editId()).isEqualTo(begin.editId());
            assertThat(update.propertyId()).isEqualTo("position");
            assertThat(selected.position()).isNotEqualTo(new Vector3f());

            SceneTransformEvent cancel = session.cancelTransform().orElseThrow();
            assertThat(cancel.phase()).isEqualTo(Phase.CANCEL);
            assertThat(cancel.editId()).isEqualTo(begin.editId());
            assertThat(selected.position()).isEqualTo(new Vector3f());
        }
    }

    @Test
    void retainsTheBeginToolAndSnapConfigurationForTheCompleteDrag() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(transformableRoot())));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point endpoint = state.endpoint(Axis.X);
            session.transform(PointerPhase.BEGIN, endpoint.x(), endpoint.y(), 1_000, 700);

            session.transformGizmo().configure(settings(SceneTransformTool.ROTATE, SceneTransformSpace.LOCAL, true));
            SceneTransformEvent update = session.transform(
                            PointerPhase.UPDATE, endpoint.x() + 31.0f, endpoint.y(), 1_000, 700)
                    .event()
                    .orElseThrow();

            assertThat(update.propertyId()).isEqualTo("position");
            assertThat(update.value().y()).isEqualByComparingTo("0");
            assertThat(update.value().z()).isEqualByComparingTo("0");
        }
    }

    @Test
    void axisSnappingPreservesUntouchedOffGridCoordinates() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            Transform3d transform = new Transform3d(
                    authoredComponent(rootOccurrence(), TRANSFORM),
                    vector("0.13", "0.27", "0.39"),
                    vector("0", "0", "0"),
                    vector("1", "1", "1"));
            session.replaceSnapshot(snapshot(
                    1L,
                    List.of(occurrence(
                            rootOccurrence(),
                            Optional.empty(),
                            ROOT_ENTITY,
                            true,
                            Optional.of(transform),
                            List.of(),
                            Optional.empty()))));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, true));
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point endpoint = state.endpoint(Axis.X);
            session.transform(PointerPhase.BEGIN, endpoint.x(), endpoint.y(), 1_000, 700);

            SceneTransformEvent update = session.transform(
                            PointerPhase.UPDATE, endpoint.x() + 31.0f, endpoint.y(), 1_000, 700)
                    .event()
                    .orElseThrow();

            assertThat(update.value().y()).isEqualByComparingTo("0.27");
            assertThat(update.value().z()).isEqualByComparingTo("0.39");
        }
    }

    @Test
    void distinguishesLocalAxesAndSnapsScaleUpdates() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(2L, List.of(rotatedTransformableRoot())));
            session.select(2L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            Vector3f globalX = new Vector3f(session.transformGizmo()
                    .renderState(1_000, 700)
                    .orElseThrow()
                    .axis(Axis.X)
                    .world());

            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.LOCAL, false));
            Vector3f localX = new Vector3f(session.transformGizmo()
                    .renderState(1_000, 700)
                    .orElseThrow()
                    .axis(Axis.X)
                    .world());
            assertThat(localX).isNotEqualTo(globalX);

            session.configureTransform(settings(SceneTransformTool.SCALE, SceneTransformSpace.LOCAL, true));
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point center = state.center();
            SceneTransformEvent begin = session.transform(PointerPhase.BEGIN, center.x(), center.y(), 1_000, 700)
                    .event()
                    .orElseThrow();
            SceneTransformEvent update = session.transform(
                            PointerPhase.UPDATE, center.x() + 17.0f, center.y() - 17.0f, 1_000, 700)
                    .event()
                    .orElseThrow();

            assertThat(begin.phase()).isEqualTo(Phase.BEGIN);
            assertThat(update.value().x()).isEqualByComparingTo("1.4");
            assertThat(update.value().y()).isEqualByComparingTo("1.4");
            assertThat(update.value().z()).isEqualByComparingTo("1.4");
        }
    }

    @Test
    void blocksInheritedTransformsAndKeepsGizmoOutOfAuthoredIdentity() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(3L, List.of(inheritedTransformRoot())));
            session.select(3L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            SceneTransformGizmoState state =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point endpoint = state.endpoint(Axis.Y);

            SceneTransformEvent blocked = session.transform(PointerPhase.BEGIN, endpoint.x(), endpoint.y(), 1_000, 700)
                    .event()
                    .orElseThrow();

            assertThat(blocked.phase()).isEqualTo(Phase.BLOCKED);
            assertThat(session.transformGizmo()).isNotInstanceOf(Object3D.class);
            assertThat(session.transformGizmo().dragging()).isFalse();
        }
    }

    @Test
    void hidesAndRestoresSelectedMeshGizmoWithoutChangingSelectionOrTool() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(meshTransformRoot(true, true))));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            SceneTransformGizmoState visible =
                    session.transformGizmo().renderState(1_000, 700).orElseThrow();
            Point previousHandle = visible.endpoint(Axis.X);

            session.replaceSnapshot(snapshot(2L, List.of(meshTransformRoot(true, false))));

            assertThat(session.transformGizmo().renderState(1_000, 700)).isEmpty();
            assertThat(session.transform(PointerPhase.BEGIN, previousHandle.x(), previousHandle.y(), 1_000, 700)
                            .claimed())
                    .isFalse();
            assertThat(session.synchronizeSelection(2L, Optional.of(rootOccurrence())))
                    .isEmpty();

            session.replaceSnapshot(snapshot(3L, List.of(meshTransformRoot(true, true))));

            assertThat(session.transformGizmo().renderState(1_000, 700))
                    .hasValueSatisfying(state -> assertThat(state.tool()).isEqualTo(SceneTransformTool.MOVE));
        }
    }

    @Test
    void evaluatesEveryMeshRendererBeforePresentingTheTransformGizmo() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(meshTransformRoot(true, true, true, true))));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.ROTATE, SceneTransformSpace.GLOBAL, false));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isPresent();

            session.replaceSnapshot(snapshot(2L, List.of(meshTransformRoot(true, false, true, false))));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isPresent();

            session.replaceSnapshot(snapshot(3L, List.of(meshTransformRoot(true, false, false, false))));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isEmpty();
        }
    }

    @Test
    void suppressesDisabledEntitiesButKeepsTransformOnlyAndLightEntitiesEligible() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(meshTransformRoot(false, true))));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isEmpty();

            session.replaceSnapshot(snapshot(2L, effectivelyDisabledChildOccurrences()));
            session.select(2L, Optional.of(childOccurrence()));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isEmpty();

            session.replaceSnapshot(snapshot(3L, List.of(transformableRoot())));
            session.select(3L, Optional.of(rootOccurrence()));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isPresent();

            session.replaceSnapshot(snapshot(4L, List.of(lightTransformRoot())));
            assertThat(session.transformGizmo().renderState(1_000, 700)).isPresent();
        }
    }

    @Test
    void keepsHiddenSelectionNonInteractiveAcrossToolChangesAndRestoresTheCurrentTool() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(meshTransformRoot(true, false))));
            session.select(1L, Optional.of(rootOccurrence()));

            for (SceneTransformTool tool : SceneTransformTool.values()) {
                session.configureTransform(settings(tool, SceneTransformSpace.GLOBAL, false));
                assertThat(session.transformGizmo().renderState(1_000, 700)).isEmpty();
            }
            assertThat(session.transform(PointerPhase.BEGIN, 500.0f, 350.0f, 1_000, 700)
                            .claimed())
                    .isFalse();
            assertThat(session.synchronizeSelection(1L, Optional.of(rootOccurrence())))
                    .isEmpty();

            session.replaceSnapshot(snapshot(2L, List.of(meshTransformRoot(true, true))));
            assertThat(session.transformGizmo().renderState(1_000, 700))
                    .hasValueSatisfying(state -> assertThat(state.tool()).isEqualTo(SceneTransformTool.SCALE));
        }
    }

    @Test
    void clearsAnActiveTransformWhenTheSelectedMeshBecomesHidden() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(1L, List.of(meshTransformRoot(true, true))));
            session.select(1L, Optional.of(rootOccurrence()));
            session.configureTransform(settings(SceneTransformTool.MOVE, SceneTransformSpace.GLOBAL, false));
            Point handle = session.transformGizmo()
                    .renderState(1_000, 700)
                    .orElseThrow()
                    .endpoint(Axis.X);
            assertThat(session.transform(PointerPhase.BEGIN, handle.x(), handle.y(), 1_000, 700)
                            .claimed())
                    .isTrue();

            session.replaceSnapshot(snapshot(2L, List.of(meshTransformRoot(true, false))));

            assertThat(session.transformGizmo().dragging()).isFalse();
            assertThat(session.transform(PointerPhase.UPDATE, handle.x() + 20.0f, handle.y(), 1_000, 700)
                            .claimed())
                    .isFalse();
        }
    }

    @Test
    void highlightsSharedSelectionAndRetainsItAcrossSnapshotReplacement() {
        TrackingResolver resolver = new TrackingResolver();
        SceneViewRendererSession session = new SceneViewRendererSession(resolver);
        session.replaceSnapshot(snapshot(1L, List.of(pickableRoot())));

        assertThat(session.select(0L, Optional.of(rootOccurrence()))).isFalse();
        assertThat(session.select(1L, Optional.of(rootOccurrence()))).isTrue();
        BoxHelper firstHelper = selectionHelper(session);
        assertThat(firstHelper.target())
                .isSameAs(session.objectFor(rootOccurrence()).orElseThrow());
        assertThat(session.identityFor(firstHelper)).isEmpty();

        session.replaceSnapshot(snapshot(2L, List.of(pickableRoot())));
        BoxHelper replacementHelper = selectionHelper(session);
        assertThat(replacementHelper).isNotSameAs(firstHelper);
        assertThat(replacementHelper.target())
                .isSameAs(session.objectFor(rootOccurrence()).orElseThrow());
        assertThat(firstHelper.parent()).isNull();
        assertThat(firstHelper.isClosed()).isTrue();

        session.close();
        assertThat(replacementHelper.parent()).isNull();
        assertThat(replacementHelper.isClosed()).isTrue();
    }

    @Test
    void picksVisibleAuthoredGeometryWithoutAddingEditorIdentity() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.replaceSnapshot(snapshot(4L, List.of(pickableRoot())));

            SceneViewSelectionResult selected = session.pick(4L, 0.0f, 0.0f);
            SceneViewSelectionResult stale = session.pick(3L, 0.0f, 0.0f);

            assertThat(selected.status()).isEqualTo(SceneViewSelectionResult.Status.SELECTED);
            assertThat(selected.occurrence()).contains(rootOccurrence());
            assertThat(stale.status()).isEqualTo(SceneViewSelectionResult.Status.STALE);
            assertThat(stale.revision()).isEqualTo(4L);
            assertThat(session.identityFor(selectionHelper(session))).isEmpty();
        }
    }

    @Test
    void reusesResourcesAndRejectsEqualAndStaleSnapshots() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            session.replaceSnapshot(snapshot(1L, visualOccurrences()));
            session.replaceSnapshot(snapshot(2L, visualOccurrences()));
            Object3D current = session.objectFor(childOccurrence()).orElseThrow();

            SceneViewRealizationResult equal = session.replaceSnapshot(snapshot(2L, List.of()));
            SceneViewRealizationResult stale = session.replaceSnapshot(snapshot(1L, List.of()));

            assertThat(equal.status()).isEqualTo(Status.UNCHANGED);
            assertThat(stale.status()).isEqualTo(Status.STALE);
            assertThat(session.revision()).hasValue(2L);
            assertThat(session.objectFor(childOccurrence())).containsSame(current);
            assertThat(resolver.meshAcquisitions()).isEqualTo(1);
            assertThat(resolver.materialAcquisitions()).isEqualTo(1);
            assertThat(resolver.releaseCount()).isZero();
        }
        assertThat(resolver.releaseCount()).isEqualTo(2);
        assertThat(resolver.geometry().isClosed()).isTrue();
        assertThat(resolver.material().isClosed()).isTrue();
        assertThat(resolver.texture().isClosed()).isTrue();
    }

    @Test
    void leavesCurrentGraphIntactWhenAResourceIsUnsupported() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            session.replaceSnapshot(snapshot(1L, visualOccurrences()));
            Object3D current = session.objectFor(childOccurrence()).orElseThrow();
            resolver.rejectMaterials();

            SceneViewRealizationResult result =
                    session.replaceSnapshot(snapshot(2L, visualOccurrences(UNSUPPORTED_MATERIAL_REFERENCE)));

            assertThat(result.status()).isEqualTo(Status.FAILED);
            assertThat(result.currentRevision()).hasValue(1L);
            assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
                assertThat(diagnostic.code()).isEqualTo(Code.RESOURCE_UNSUPPORTED);
                assertThat(diagnostic.resource()).contains(UNSUPPORTED_MATERIAL_REFERENCE);
            });
            assertThat(session.objectFor(childOccurrence())).containsSame(current);
            assertThat(resolver.releaseCount()).isZero();
        }
    }

    @Test
    void rejectsSnapshotsForAnotherSceneWithoutChangingCurrentContent() {
        TrackingResolver resolver = new TrackingResolver();
        try (SceneViewRendererSession session = new SceneViewRendererSession(resolver)) {
            session.replaceSnapshot(snapshot(1L, List.of(root())));
            Object3D current = session.objectFor(rootOccurrence()).orElseThrow();
            AssetId otherScene = AssetId.from("99999999-9999-4999-8999-999999999999");

            SceneViewRealizationResult result =
                    session.replaceSnapshot(new SceneViewSnapshot(otherScene, 2L, List.of()));

            assertThat(result.status()).isEqualTo(Status.FAILED);
            assertThat(result.diagnostics())
                    .extracting(SceneViewRealizationResult.Diagnostic::code)
                    .containsExactly(Code.SCENE_MISMATCH);
            assertThat(session.objectFor(rootOccurrence())).containsSame(current);
        }
    }

    private static void assertRotation(Object3D object) {
        Quaternionf expected = new Quaternionf()
                .rotationXYZ((float) Math.toRadians(10.0), (float) Math.toRadians(20.0), (float) Math.toRadians(30.0));
        assertThat(object.quaternion().x()).isCloseTo(expected.x, offset(0.000_001f));
        assertThat(object.quaternion().y()).isCloseTo(expected.y, offset(0.000_001f));
        assertThat(object.quaternion().z()).isCloseTo(expected.z, offset(0.000_001f));
        assertThat(object.quaternion().w()).isCloseTo(expected.w, offset(0.000_001f));
    }

    private static List<VisualOccurrence> visualOccurrences() {
        return visualOccurrences(MATERIAL_REFERENCE);
    }

    private static List<VisualOccurrence> visualOccurrences(ResourceReference material) {
        return List.of(
                root(),
                visualChild(childOccurrence(), false, material),
                visualChild(secondChildOccurrence(), true, material));
    }

    private static VisualOccurrence root() {
        return occurrence(
                rootOccurrence(), Optional.empty(), ROOT_ENTITY, true, Optional.empty(), List.of(), Optional.empty());
    }

    private static VisualOccurrence pickableRoot() {
        MeshRenderer3d mesh =
                new MeshRenderer3d(component(rootOccurrence(), MESH), MESH_REFERENCE, MATERIAL_REFERENCE, true);
        return occurrence(
                rootOccurrence(),
                Optional.empty(),
                ROOT_ENTITY,
                true,
                Optional.empty(),
                List.of(mesh),
                Optional.empty());
    }

    private static VisualOccurrence transformableRoot() {
        return transformRoot(vector("0", "0", "0"), authoredComponent(rootOccurrence(), TRANSFORM));
    }

    private static VisualOccurrence meshTransformRoot(boolean enabled, boolean... meshVisibility) {
        Transform3d transform = new Transform3d(
                authoredComponent(rootOccurrence(), TRANSFORM),
                vector("0", "0", "0"),
                vector("0", "0", "0"),
                vector("1", "1", "1"));
        List<ComponentId> meshIds = List.of(MESH, SECOND_MESH, THIRD_MESH);
        List<MeshRenderer3d> meshes = new ArrayList<>();
        for (int index = 0; index < meshVisibility.length; index++) {
            meshes.add(new MeshRenderer3d(
                    authoredComponent(rootOccurrence(), meshIds.get(index)),
                    MESH_REFERENCE,
                    MATERIAL_REFERENCE,
                    meshVisibility[index]));
        }
        return occurrence(
                rootOccurrence(),
                Optional.empty(),
                ROOT_ENTITY,
                enabled,
                Optional.of(transform),
                meshes,
                Optional.empty());
    }

    private static VisualOccurrence lightTransformRoot() {
        Transform3d transform = new Transform3d(
                authoredComponent(rootOccurrence(), TRANSFORM),
                vector("0", "0", "0"),
                vector("0", "0", "0"),
                vector("1", "1", "1"));
        DirectionalLight3d light = new DirectionalLight3d(
                authoredComponent(rootOccurrence(), LIGHT), vector("1", "1", "1"), decimal("1"), vector("0", "0", "0"));
        return occurrence(
                rootOccurrence(),
                Optional.empty(),
                ROOT_ENTITY,
                true,
                Optional.of(transform),
                List.of(),
                Optional.of(light));
    }

    private static List<VisualOccurrence> effectivelyDisabledChildOccurrences() {
        VisualOccurrence parent = occurrence(
                rootOccurrence(), Optional.empty(), ROOT_ENTITY, false, Optional.empty(), List.of(), Optional.empty());
        Transform3d transform = new Transform3d(
                authoredComponent(childOccurrence(), TRANSFORM),
                vector("0", "0", "0"),
                vector("0", "0", "0"),
                vector("1", "1", "1"));
        MeshRenderer3d mesh = new MeshRenderer3d(
                authoredComponent(childOccurrence(), MESH), MESH_REFERENCE, MATERIAL_REFERENCE, true);
        VisualOccurrence child = occurrence(
                childOccurrence(),
                Optional.of(rootOccurrence()),
                CHILD_ENTITY,
                false,
                Optional.of(transform),
                List.of(mesh),
                Optional.empty());
        return List.of(parent, child);
    }

    private static VisualOccurrence rotatedTransformableRoot() {
        return transformRoot(vector("0", "0", "90"), authoredComponent(rootOccurrence(), TRANSFORM));
    }

    private static VisualOccurrence inheritedTransformRoot() {
        return transformRoot(vector("0", "0", "0"), component(rootOccurrence(), TRANSFORM));
    }

    private static VisualOccurrence transformRoot(Vector3 orientation, ComponentIdentity identity) {
        Transform3d transform = new Transform3d(identity, vector("0", "0", "0"), orientation, vector("1", "1", "1"));
        return occurrence(
                rootOccurrence(),
                Optional.empty(),
                ROOT_ENTITY,
                true,
                Optional.of(transform),
                List.of(),
                Optional.empty());
    }

    private static BoxHelper selectionHelper(SceneViewRendererSession session) {
        return session.scene().children().stream()
                .filter(BoxHelper.class::isInstance)
                .map(BoxHelper.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static VisualOccurrence visualChild(
            CompositionOccurrenceId occurrence, boolean enabled, ResourceReference materialReference) {
        ComponentIdentity transformIdentity = component(occurrence, TRANSFORM);
        Transform3d transform = new Transform3d(
                transformIdentity, vector("1", "2", "3"), vector("10", "20", "30"), vector("2", "3", "4"));
        MeshRenderer3d mesh = new MeshRenderer3d(component(occurrence, MESH), MESH_REFERENCE, materialReference, false);
        DirectionalLight3d light = new DirectionalLight3d(
                component(occurrence, LIGHT), vector("0.25", "0.5", "0.75"), decimal("3.5"), vector("7", "8", "9"));
        EntityId authoredEntity = occurrence.equals(childOccurrence()) ? CHILD_ENTITY : SECOND_CHILD_ENTITY;
        return occurrence(
                occurrence,
                Optional.of(rootOccurrence()),
                authoredEntity,
                enabled,
                Optional.of(transform),
                List.of(mesh),
                Optional.of(light));
    }

    private static VisualOccurrence occurrence(
            CompositionOccurrenceId occurrence,
            Optional<CompositionOccurrenceId> parent,
            EntityId authoredEntity,
            boolean enabled,
            Optional<Transform3d> transform,
            List<MeshRenderer3d> meshes,
            Optional<DirectionalLight3d> light) {
        return new VisualOccurrence(
                occurrence,
                parent,
                DEFINITION,
                SOURCE,
                authoredEntity,
                Optional.of("Visual"),
                enabled,
                transform,
                meshes,
                light,
                List.of());
    }

    private static VisualOccurrence authoredCameraRoot(PerspectiveCamera3d camera) {
        return new VisualOccurrence(
                rootOccurrence(),
                Optional.empty(),
                SCENE,
                SOURCE,
                ROOT_ENTITY,
                Optional.of("Camera"),
                true,
                Optional.empty(),
                List.of(),
                Optional.empty(),
                List.of(camera));
    }

    private static SceneViewSnapshot snapshot(long revision, List<VisualOccurrence> occurrences) {
        return new SceneViewSnapshot(SCENE, revision, occurrences);
    }

    private static ComponentIdentity component(CompositionOccurrenceId occurrence, ComponentId component) {
        return new ComponentIdentity(
                occurrence,
                new CompositionScope(DEFINITION, occurrence),
                occurrence.entityPath().getLast(),
                component);
    }

    private static ComponentIdentity authoredComponent(CompositionOccurrenceId occurrence, ComponentId component) {
        return new ComponentIdentity(
                occurrence,
                new CompositionScope(SCENE, occurrence),
                occurrence.entityPath().getLast(),
                component);
    }

    private static SceneTransformSettings settings(
            SceneTransformTool tool, SceneTransformSpace space, boolean snapping) {
        return new SceneTransformSettings(tool, space, snapping, 0.5f, 15.0f, 0.2f);
    }

    private static void assertHandleTransactions(SceneTransformTool tool, String property, List<Handle> handles) {
        for (Handle handle : handles) {
            try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
                session.replaceSnapshot(snapshot(1L, List.of(transformableRoot())));
                session.select(1L, Optional.of(rootOccurrence()));
                session.configureTransform(settings(tool, SceneTransformSpace.GLOBAL, false));
                SceneTransformGizmoState state =
                        session.transformGizmo().renderState(1_000, 700).orElseThrow();
                Point start = findHandle(state, handle);

                SceneTransformEvent begin = session.transform(PointerPhase.BEGIN, start.x(), start.y(), 1_000, 700)
                        .event()
                        .orElseThrow();
                SceneTransformEvent update = session.transform(
                                PointerPhase.UPDATE, start.x() + 37.0f, start.y() - 23.0f, 1_000, 700)
                        .event()
                        .orElseThrow();
                SceneTransformEvent commit = session.transform(
                                PointerPhase.COMMIT, start.x() + 37.0f, start.y() - 23.0f, 1_000, 700)
                        .event()
                        .orElseThrow();

                assertThat(begin.phase()).isEqualTo(Phase.BEGIN);
                assertThat(update.phase()).isEqualTo(Phase.UPDATE);
                assertThat(commit.phase()).isEqualTo(Phase.COMMIT);
                assertThat(update.editId()).isEqualTo(begin.editId());
                assertThat(commit.editId()).isEqualTo(begin.editId());
                assertThat(commit.propertyId()).isEqualTo(property);
                assertThat(session.transformGizmo().dragging()).isFalse();
            }
        }
    }

    private static Point findHandle(SceneTransformGizmoState state, Handle expected) {
        for (float y = state.center().y() - 160.0f; y <= state.center().y() + 160.0f; y += 2.0f) {
            for (float x = state.center().x() - 160.0f; x <= state.center().x() + 160.0f; x += 2.0f) {
                if (state.hit(x, y).filter(expected::equals).isPresent()) {
                    return new Point(x, y);
                }
            }
        }
        throw new AssertionError("No pickable point for " + expected + " in " + state.tool());
    }

    private static float distance(Point first, Point second) {
        return (float) Math.hypot(second.x() - first.x(), second.y() - first.y());
    }

    private static void assertExcludedPlaneCoordinate(Handle handle, Vector3 value) {
        BigDecimal excluded =
                switch (handle) {
                    case XY -> value.z();
                    case XZ -> value.y();
                    case YZ -> value.x();
                    case X, Y, Z, UNIFORM -> throw new IllegalArgumentException("handle is not a plane");
                };
        assertThat(excluded.floatValue()).isCloseTo(0.0f, offset(1.0e-5f));
    }

    private static float vectorLength(Vector3 value) {
        float x = value.x().floatValue();
        float y = value.y().floatValue();
        float z = value.z().floatValue();
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private static float cameraDistance(SceneViewCameraState state) {
        return new Vector3f(state.position()).sub(state.pivot()).length();
    }

    private static CompositionOccurrenceId rootOccurrence() {
        return new CompositionOccurrenceId(SCENE, List.of(ROOT_ENTITY));
    }

    private static CompositionOccurrenceId childOccurrence() {
        return rootOccurrence().child(CHILD_ENTITY);
    }

    private static CompositionOccurrenceId secondChildOccurrence() {
        return rootOccurrence().child(SECOND_CHILD_ENTITY);
    }

    private static Vector3 vector(String x, String y, String z) {
        return new Vector3(decimal(x), decimal(y), decimal(z));
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    /** Fixed product-value resolver with observable lease ownership. */
    private static final class TrackingResolver implements SceneViewResourceResolver {
        private final BufferGeometry geometry = BoxGeometry.create(1.0f, 1.0f, 1.0f);
        private final Texture texture = Texture.baseColor(1, 1, new byte[] {(byte) 255, 0, 0, (byte) 255});
        private final BasicMaterial material = new BasicMaterial();
        private final Map<ResourceReference, Integer> acquisitions = new HashMap<>();
        private final List<ResourceReference> releases = new ArrayList<>();
        private boolean rejectMaterials;

        private TrackingResolver() {
            material.setColorMap(texture);
        }

        @Override
        public Lease<BufferGeometry> acquireMesh(ResourceReference reference) {
            acquisitions.merge(reference, 1, Integer::sum);
            return new TrackingLease<>(geometry, () -> {
                releases.add(reference);
                geometry.close();
            });
        }

        @Override
        public Lease<Material> acquireMaterial(ResourceReference reference) {
            if (rejectMaterials) {
                throw new UnsupportedSceneViewResourceException("unsupported material");
            }
            acquisitions.merge(reference, 1, Integer::sum);
            return new TrackingLease<>(material, () -> {
                releases.add(reference);
                material.close();
                texture.close();
            });
        }

        private int acquisitionCount() {
            return acquisitions.values().stream().mapToInt(Integer::intValue).sum();
        }

        private int meshAcquisitions() {
            return acquisitions.getOrDefault(MESH_REFERENCE, 0);
        }

        private int materialAcquisitions() {
            return acquisitions.getOrDefault(MATERIAL_REFERENCE, 0);
        }

        private int releaseCount() {
            return releases.size();
        }

        private BufferGeometry geometry() {
            return geometry;
        }

        private BasicMaterial material() {
            return material;
        }

        private Texture texture() {
            return texture;
        }

        private void rejectMaterials() {
            rejectMaterials = true;
        }
    }

    /** Minimal idempotent test lease. */
    private static final class TrackingLease<T> implements SceneViewResourceResolver.Lease<T> {
        private final T value;
        private final Runnable release;
        private boolean closed;

        private TrackingLease(T value, Runnable release) {
            this.value = value;
            this.release = release;
        }

        @Override
        public T value() {
            return value;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                release.run();
            }
        }
    }
}
