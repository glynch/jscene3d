/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Code;
import io.github.glynch.jscene3d.editor.renderer.process.SceneViewRealizationResult.Status;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.ComponentIdentity;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.DirectionalLight3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.MeshRenderer3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Transform3d;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.Vector3;
import io.github.glynch.jscene3d.editor.workbench.sceneview.SceneViewSnapshot.VisualOccurrence;
import io.github.glynch.jscene3d.geometries.BoxGeometry;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.helpers.BoxHelper;
import io.github.glynch.jscene3d.lights.DirectionalLight;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.Material;
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
    private static final ComponentId LIGHT = ComponentId.from("88888888-8888-4888-8888-888888888888");
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
                    .contains(session.camera(), session.grid())
                    .hasSize(3);
            assertThat(session.orientationGizmo()).isNotInstanceOf(Object3D.class);
            assertThat(session.identityFor(session.camera())).isEmpty();
            assertThat(session.identityFor(session.grid())).isEmpty();
            assertThat(resolver.acquisitionCount()).isZero();
        }
    }

    @Test
    void updatesEditorCameraAspectFromTheCurrentPhysicalViewport() {
        try (SceneViewRendererSession session = new SceneViewRendererSession(new TrackingResolver())) {
            session.updateViewportSize(1_920, 1_080);
            assertThat(session.camera().aspectRatio()).isCloseTo(16.0f / 9.0f, offset(0.000_001f));

            session.updateViewportSize(900, 1_600);
            assertThat(session.camera().aspectRatio()).isCloseTo(9.0f / 16.0f, offset(0.000_001f));
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
                light);
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
