/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.raycasting;

import static io.github.glynch.jscene3d.testing.JomlAssertions.EPSILON;
import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.materials.BasicMaterial;
import io.github.glynch.jscene3d.materials.LineBasicMaterial;
import io.github.glynch.jscene3d.materials.MaterialSide;
import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.objects.Group;
import io.github.glynch.jscene3d.objects.InstancedMesh;
import io.github.glynch.jscene3d.objects.Line;
import io.github.glynch.jscene3d.objects.Mesh;
import io.github.glynch.jscene3d.scenes.Scene;
import java.util.List;
import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class MeshRaycastingTest {
    @Test
    void intersectsActiveInstancesAndReportsTheirIndices() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            InstancedMesh mesh = new InstancedMesh(geometry, material, 3);
            mesh.setMatrixAt(0, new Matrix4f().translation(-3.0f, 0.0f, 0.0f));
            mesh.setMatrixAt(1, new Matrix4f().translation(0.0f, 0.0f, -1.0f));
            mesh.setMatrixAt(2, new Matrix4f().translation(0.0f, 0.0f, -2.0f));
            mesh.setCount(2);

            List<RaycastHit> hits = frontRay(2.0f).intersect(mesh);

            assertThat(hits).hasSize(1);
            assertThat(hits.getFirst().mesh()).isSameAs(mesh);
            assertThat(hits.getFirst().instanceIndex()).hasValue(1);
            assertThat(hits.getFirst().distance()).isCloseTo(3.0f, within(EPSILON));
        }
    }

    @Test
    void intersectsNonIndexedGeometryAndReturnsImmutableHitValues() {
        try (BufferGeometry geometry = triangleWithTextureCoordinates();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            Raycaster raycaster = frontRay(2.0f);

            List<RaycastHit> hits = raycaster.intersect(mesh);

            assertThat(hits).hasSize(1);
            RaycastHit hit = hits.getFirst();
            assertThat(hit.mesh()).isSameAs(mesh);
            assertThat(hit.distance()).isCloseTo(2.0f, within(EPSILON));
            assertThat(hit.faceIndex()).isZero();
            assertVector(hit.point(new Vector3f()), 0.0f, 0.0f, 0.0f);
            assertThat(hit.hasTextureCoordinate()).isTrue();
            Vector2f textureCoordinate = hit.textureCoordinate(new Vector2f());
            assertThat(textureCoordinate.x()).isCloseTo(0.5f, within(EPSILON));
            assertThat(textureCoordinate.y()).isCloseTo(0.5f, within(EPSILON));

            textureCoordinate.zero();
            assertThat(hit.textureCoordinate(new Vector2f()).x()).isCloseTo(0.5f, within(EPSILON));
            assertThatThrownByUnsupportedMutation(hits, hit);
        }
    }

    @Test
    void intersectsIndexedGeometryWithinItsDrawRangeAndSortsFaces() {
        try (BufferGeometry geometry = twoIndexedTriangles();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            Raycaster raycaster = frontRay(3.0f);
            geometry.setDrawRange(3, 3);

            List<RaycastHit> selectedHits = raycaster.intersect(mesh);
            assertThat(selectedHits).hasSize(1);
            assertThat(selectedHits.getFirst().faceIndex()).isOne();
            assertThat(selectedHits.getFirst().distance()).isCloseTo(3.0f, within(EPSILON));

            geometry.clearDrawRange();
            List<RaycastHit> allHits = raycaster.intersect(mesh);
            assertThat(allHits).extracting(RaycastHit::faceIndex).containsExactly(0, 1);
            assertThat(allHits).extracting(RaycastHit::distance).containsExactly(2.0f, 3.0f);
        }
    }

    @Test
    void appliesHierarchyTransformsAndKeepsWorldDistanceMeaningful() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Group parent = new Group();
            parent.setPosition(1.0f, 2.0f, 0.0f);
            parent.setScale(2.0f, 3.0f, 1.0f);
            Mesh mesh = new Mesh(geometry, material);
            parent.add(mesh);
            Raycaster raycaster = new Raycaster(new Vector3f(1.0f, 2.0f, 4.0f), new Vector3f(0.0f, 0.0f, -2.0f));

            RaycastHit hit = raycaster.intersect(parent).getFirst();

            assertThat(hit.distance()).isCloseTo(4.0f, within(EPSILON));
            assertVector(hit.point(new Vector3f()), 1.0f, 2.0f, 0.0f);
        }
    }

    @Test
    void respectsFrontBackAndDoubleSidedMaterials() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            Raycaster front = frontRay(2.0f);
            Raycaster back = new Raycaster(new Vector3f(0.0f, 0.0f, -2.0f), new Vector3f(0.0f, 0.0f, 1.0f));

            assertThat(front.intersect(mesh)).hasSize(1);
            assertThat(back.intersect(mesh)).isEmpty();

            material.setSide(MaterialSide.BACK);
            assertThat(front.intersect(mesh)).isEmpty();
            assertThat(back.intersect(mesh)).hasSize(1);

            material.setSide(MaterialSide.DOUBLE);
            assertThat(front.intersect(mesh)).hasSize(1);
            assertThat(back.intersect(mesh)).hasSize(1);
        }
    }

    @Test
    void accountsForWorldTransformsThatReverseTriangleWinding() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            mesh.setScale(-1.0f, 1.0f, 1.0f);
            Raycaster raycaster = frontRay(2.0f);

            assertThat(raycaster.intersect(mesh)).isEmpty();

            material.setSide(MaterialSide.BACK);
            assertThat(raycaster.intersect(mesh)).hasSize(1);
        }
    }

    @Test
    void traversesVisibleBranchesAndReturnsNearestHitsFirst() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial nearMaterial = new BasicMaterial();
                BasicMaterial farMaterial = new BasicMaterial()) {
            Scene scene = new Scene();
            Group group = new Group();
            Mesh far = new Mesh(geometry, farMaterial);
            Mesh near = new Mesh(geometry, nearMaterial);
            far.setPosition(0.0f, 0.0f, -1.0f);
            near.setPosition(0.0f, 0.0f, 1.0f);
            group.add(far);
            group.add(near);
            scene.add(group);
            Raycaster raycaster = frontRay(4.0f);

            assertThat(raycaster.intersect(scene)).extracting(RaycastHit::mesh).containsExactly(near, far);
            assertThat(raycaster.intersect(scene, false)).isEmpty();

            group.setVisible(false);
            assertThat(raycaster.intersect(scene)).isEmpty();
            group.setVisible(true);
            nearMaterial.setVisible(false);
            assertThat(raycaster.intersect(scene)).extracting(RaycastHit::mesh).containsExactly(far);
        }
    }

    @Test
    void retainsTraversalOrderWhenHitsHaveEqualDistance() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh first = new Mesh(geometry, material);
            Mesh second = new Mesh(geometry, material);
            Scene scene = new Scene();
            scene.add(first);
            scene.add(second);

            assertThat(frontRay(2.0f).intersect(scene))
                    .extracting(RaycastHit::mesh)
                    .containsExactly(first, second);
        }
    }

    @Test
    void traversesArtificiallyDeepHierarchiesWithoutUsingTheJavaStack() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Group root = new Group();
            Group parent = root;
            for (int depth = 0; depth < 10_000; depth++) {
                Group child = new Group();
                parent.add(child);
                parent = child;
            }
            Mesh mesh = new Mesh(geometry, material);
            parent.add(mesh);

            assertThat(frontRay(2.0f).intersect(root))
                    .extracting(RaycastHit::mesh)
                    .containsExactly(mesh);
        }
    }

    @Test
    void usesSuppliedSphereAndBoxBoundsForBroadPhaseRejection() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            Raycaster raycaster = frontRay(2.0f);
            geometry.setBoundingSphere(new BoundingSphere(100.0f, 0.0f, 0.0f, 1.0f));

            assertThat(raycaster.intersect(mesh)).isEmpty();

            geometry.setBoundingSphere(new BoundingSphere(0.0f, 0.0f, 0.0f, 2.0f));
            geometry.setBoundingBox(new BoundingBox(100.0f, -1.0f, -1.0f, 101.0f, 1.0f, 1.0f));
            assertThat(raycaster.intersect(mesh)).isEmpty();
        }
    }

    @Test
    void refreshesComputedBoundsAfterPositionEdits() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            Raycaster centerRay = frontRay(2.0f);
            assertThat(centerRay.intersect(mesh)).hasSize(1);
            BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));

            positions.edit(editor -> {
                for (int index = 0; index < positions.count(); index++) {
                    editor.setX(index, positions.value(index, 0) + 10.0f);
                }
            });

            assertThat(centerRay.intersect(mesh)).isEmpty();
            Raycaster movedRay = new Raycaster(new Vector3f(10.0f, 0.0f, 2.0f), new Vector3f(0.0f, 0.0f, -1.0f));
            assertThat(movedRay.intersect(mesh)).hasSize(1);
        }
    }

    @Test
    void reportsMissingTextureCoordinatesAndIgnoresLines() {
        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh mesh = new Mesh(geometry, material);
            RaycastHit hit = frontRay(2.0f).intersect(mesh).getFirst();

            assertThat(hit.hasTextureCoordinate()).isFalse();
            assertThatIllegalStateException().isThrownBy(() -> hit.textureCoordinate(new Vector2f()));

            try (LineBasicMaterial lineMaterial = new LineBasicMaterial()) {
                assertThat(frontRay(2.0f).intersect(new Line(geometry, lineMaterial)))
                        .isEmpty();
            }
        }
    }

    @Test
    void failsClearlyForInvalidMeshState() {
        Raycaster raycaster = frontRay(2.0f);
        try (BufferGeometry emptyGeometry = new BufferGeometry();
                BasicMaterial emptyMaterial = new BasicMaterial()) {
            assertThat(raycaster.intersect(new Mesh(emptyGeometry, emptyMaterial)))
                    .isEmpty();
        }

        try (BufferGeometry geometry = triangle();
                BasicMaterial material = new BasicMaterial()) {
            Mesh singular = new Mesh(geometry, material);
            singular.setScale(0.0f, 1.0f, 1.0f);
            assertThatIllegalStateException()
                    .isThrownBy(() -> raycaster.intersect(singular))
                    .withMessageContaining("finite and invertible");
        }

        assertThatNullPointerException().isThrownBy(() -> raycaster.intersect(null));
    }

    /** Creates a normalized ray through the center of the positive-Z side. */
    private static Raycaster frontRay(float originZ) {
        return new Raycaster(new Vector3f(0.0f, 0.0f, originZ), new Vector3f(0.0f, 0.0f, -1.0f));
    }

    /** Creates one counter-clockwise positive-Z triangle. */
    private static BufferGeometry triangle() {
        return BufferGeometry.builder()
                .positions(-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
                .build();
    }

    /** Creates one triangle with interpolatable UV data. */
    private static BufferGeometry triangleWithTextureCoordinates() {
        return BufferGeometry.builder()
                .positions(-1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
                .uvs(0.0f, 0.0f, 1.0f, 0.0f, 0.5f, 1.0f)
                .build();
    }

    /** Creates two indexed coplanar-screen triangles at different Z positions. */
    private static BufferGeometry twoIndexedTriangles() {
        return BufferGeometry.builder()
                .positions(
                        -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 1.0f, -1.0f, -1.0f, 0.0f, 1.0f, -1.0f, 0.0f,
                        0.0f, 1.0f, 0.0f)
                .indices(0, 1, 2, 3, 4, 5)
                .build();
    }

    /** Verifies that a returned list cannot be structurally changed. */
    private static void assertThatThrownByUnsupportedMutation(List<RaycastHit> hits, RaycastHit hit) {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> hits.add(hit));
    }
}
