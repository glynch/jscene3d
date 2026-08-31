/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static io.github.glynch.jscene3d.math.Angles.PI;
import static io.github.glynch.jscene3d.math.Angles.PI_OVER_TWO;
import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.testing.JomlAssertions;
import java.util.Objects;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class AdditionalGeometryFactoriesTest {
    @Test
    void createsCircleWithExpectedShapeBoundsAndWinding() {
        try (BufferGeometry circle = CircleGeometry.create(2.0f, 8)) {
            assertGeometryShape(circle, 10, 24);
            BoundingBox bounds = Objects.requireNonNull(circle.boundingBox());
            assertVector(bounds.minimum(), -2.0f, -2.0f, 0.0f);
            assertVector(bounds.maximum(), 2.0f, 2.0f, 0.0f);
            assertAllTrianglesFaceExpectedNormals(circle);
        }
    }

    @Test
    void createsCircleSectorWithExpectedEndpoints() {
        try (BufferGeometry sector = CircleGeometry.create(2.0f, 4, PI_OVER_TWO, PI)) {
            BufferAttribute positions = Objects.requireNonNull(sector.attribute(BufferGeometry.POSITION));
            assertVector(position(positions, 1), 0.0f, 2.0f, 0.0f);
            assertVector(position(positions, 5), 0.0f, -2.0f, 0.0f);
        }
    }

    @Test
    void createsClosedCylinderWithExpectedShapeBoundsAndWinding() {
        try (BufferGeometry cylinder = CylinderGeometry.create(1.0f, 2.0f, 3.0f, 8, 2, false)) {
            assertGeometryShape(cylinder, 47, 144);
            BoundingBox bounds = Objects.requireNonNull(cylinder.boundingBox());
            assertVector(bounds.minimum(), -2.0f, -1.5f, -2.0f);
            assertVector(bounds.maximum(), 2.0f, 1.5f, 2.0f);
            assertAllTrianglesFaceExpectedNormals(cylinder);
        }
    }

    @Test
    void createsOpenCylinderFromCompleteOptions() {
        CylinderGeometry.Options options = new CylinderGeometry.Options(1.0f, 1.0f, 2.0f, 6, 1, true, PI_OVER_TWO, PI);
        try (BufferGeometry cylinder = CylinderGeometry.create(options)) {
            assertGeometryShape(cylinder, 14, 36);
            assertAllTrianglesFaceExpectedNormals(cylinder);
        }
    }

    @Test
    void createsConeWithoutDegenerateApexTriangles() {
        try (BufferGeometry cone = ConeGeometry.create(2.0f, 3.0f, 8, 2, false)) {
            assertGeometryShape(cone, 37, 96);
            BoundingBox bounds = Objects.requireNonNull(cone.boundingBox());
            assertVector(bounds.minimum(), -2.0f, -1.5f, -2.0f);
            assertVector(bounds.maximum(), 2.0f, 1.5f, 2.0f);
            assertAllTrianglesFaceExpectedNormals(cone);
        }
    }

    @Test
    void createsConeFromCompleteOptions() {
        ConeGeometry.Options options = new ConeGeometry.Options(1.0f, 2.0f, 6, 1, true, 0.0f, PI);
        try (BufferGeometry cone = ConeGeometry.create(options)) {
            assertGeometryShape(cone, 14, 18);
            assertAllTrianglesFaceExpectedNormals(cone);
        }
    }

    @Test
    void createsTorusWithExpectedShapeBoundsAndWinding() {
        try (BufferGeometry torus = TorusGeometry.create(3.0f, 1.0f, 4, 8)) {
            assertGeometryShape(torus, 45, 192);
            BoundingBox bounds = Objects.requireNonNull(torus.boundingBox());
            assertVector(bounds.minimum(), -4.0f, -4.0f, -1.0f);
            assertVector(bounds.maximum(), 4.0f, 4.0f, 1.0f);
            BoundingSphere sphere = Objects.requireNonNull(torus.boundingSphere());
            assertVector(sphere.center(), 0.0f, 0.0f, 0.0f);
            assertThat(sphere.radius()).isCloseTo(4.0f, within(JomlAssertions.EPSILON));
            assertAllTrianglesFaceExpectedNormals(torus);
        }
    }

    @Test
    void usesDocumentedDefaultSegmentCounts() {
        try (BufferGeometry circle = CircleGeometry.create(1.0f);
                BufferGeometry cylinder = CylinderGeometry.create(1.0f, 2.0f);
                BufferGeometry cone = ConeGeometry.create(1.0f, 2.0f);
                BufferGeometry torus = TorusGeometry.create(2.0f, 0.5f)) {
            assertThat(circle.vertexCount()).isEqualTo(34);
            assertThat(Objects.requireNonNull(circle.index()).count()).isEqualTo(96);
            assertThat(cylinder.vertexCount()).isEqualTo(134);
            assertThat(Objects.requireNonNull(cylinder.index()).count()).isEqualTo(384);
            assertThat(cone.vertexCount()).isEqualTo(100);
            assertThat(Objects.requireNonNull(cone.index()).count()).isEqualTo(192);
            assertThat(torus.vertexCount()).isEqualTo(637);
            assertThat(Objects.requireNonNull(torus.index()).count()).isEqualTo(3456);
        }
    }

    @Test
    void rejectsInvalidCircleParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> CircleGeometry.create(0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> CircleGeometry.create(1.0f, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> CircleGeometry.create(1.0f, 8, 0.0f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> CircleGeometry.create(1.0f, Integer.MAX_VALUE, 0.0f, PI));
    }

    @Test
    void rejectsInvalidCylinderParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> CylinderGeometry.create(0.0f, 0.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> CylinderGeometry.create(-1.0f, 1.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> CylinderGeometry.create(1.0f, 1.0f, 0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> CylinderGeometry.create(1.0f, 1.0f, 1.0f, 2, 1, false));
        assertThatIllegalArgumentException().isThrownBy(() -> CylinderGeometry.create(1.0f, 1.0f, 1.0f, 8, 0, false));
        CylinderGeometry.Options excessive =
                new CylinderGeometry.Options(1.0f, 1.0f, 1.0f, Integer.MAX_VALUE, 1, false, 0.0f, PI);
        assertThatIllegalArgumentException().isThrownBy(() -> CylinderGeometry.create(excessive));
        assertThatNullPointerException().isThrownBy(() -> CylinderGeometry.create((CylinderGeometry.Options) null));
    }

    @Test
    void rejectsInvalidConeParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> ConeGeometry.create(0.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> ConeGeometry.create(1.0f, Float.NaN));
        assertThatIllegalArgumentException().isThrownBy(() -> ConeGeometry.create(1.0f, 1.0f, 2));
        assertThatNullPointerException().isThrownBy(() -> ConeGeometry.create((ConeGeometry.Options) null));
    }

    @Test
    void rejectsInvalidTorusParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> TorusGeometry.create(0.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> TorusGeometry.create(1.0f, -1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> TorusGeometry.create(1.0f, 2.0f, 2, 8));
        assertThatIllegalArgumentException().isThrownBy(() -> TorusGeometry.create(1.0f, 2.0f, 4, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> TorusGeometry.create(1.0f, 2.0f, 4, 8, 0.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> TorusGeometry.create(1.0f, 2.0f, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    private static void assertGeometryShape(BufferGeometry geometry, int vertexCount, int indexCount) {
        BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
        BufferAttribute normals = Objects.requireNonNull(geometry.attribute(BufferGeometry.NORMAL));
        BufferAttribute textureCoordinates = Objects.requireNonNull(geometry.attribute(BufferGeometry.UV));
        IndexBuffer indexBuffer = Objects.requireNonNull(geometry.index());
        assertThat(positions.count()).isEqualTo(vertexCount);
        assertThat(positions.itemSize()).isEqualTo(3);
        assertThat(normals.count()).isEqualTo(vertexCount);
        assertThat(normals.itemSize()).isEqualTo(3);
        assertThat(textureCoordinates.count()).isEqualTo(vertexCount);
        assertThat(textureCoordinates.itemSize()).isEqualTo(2);
        assertThat(indexBuffer.count()).isEqualTo(indexCount);
    }

    private static void assertAllTrianglesFaceExpectedNormals(BufferGeometry geometry) {
        BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
        BufferAttribute normals = Objects.requireNonNull(geometry.attribute(BufferGeometry.NORMAL));
        IndexBuffer indices = Objects.requireNonNull(geometry.index());
        for (int offset = 0; offset < indices.count(); offset += 3) {
            int firstIndex = indices.value(offset);
            int secondIndex = indices.value(offset + 1);
            int thirdIndex = indices.value(offset + 2);
            Vector3f first = position(positions, firstIndex);
            Vector3f second = position(positions, secondIndex);
            Vector3f third = position(positions, thirdIndex);
            Vector3f faceNormal = second.sub(first, new Vector3f()).cross(third.sub(first, new Vector3f()));
            Vector3f expectedNormal = new Vector3f(
                    normals.value(firstIndex, 0), normals.value(firstIndex, 1), normals.value(firstIndex, 2));
            assertThat(faceNormal.lengthSquared()).isPositive();
            assertThat(faceNormal.dot(expectedNormal)).isPositive();
        }
    }

    private static Vector3f position(BufferAttribute positions, int vertexIndex) {
        return new Vector3f(
                positions.value(vertexIndex, 0), positions.value(vertexIndex, 1), positions.value(vertexIndex, 2));
    }
}
