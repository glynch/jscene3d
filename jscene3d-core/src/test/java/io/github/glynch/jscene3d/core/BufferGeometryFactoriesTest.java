/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static io.github.glynch.jscene3d.core.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.util.Objects;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class BufferGeometryFactoriesTest {
    @Test
    void createsPlaneWithExpectedAttributesBoundsAndWinding() {
        try (BufferGeometry plane = PlaneGeometry.create(4.0f, 2.0f)) {
            assertGeometryShape(plane, 4, 6);
            BoundingBox box = Objects.requireNonNull(plane.boundingBox());
            assertVector(box.minimum(), -2.0f, -1.0f, 0.0f);
            assertVector(box.maximum(), 2.0f, 1.0f, 0.0f);
            assertAllTrianglesFaceExpectedNormals(plane);
        }
    }

    @Test
    void createsBoxWithIndependentFaceVerticesAndOutwardWinding() {
        try (BufferGeometry boxGeometry = BoxGeometry.create(2.0f, 4.0f, 6.0f)) {
            assertGeometryShape(boxGeometry, 24, 36);
            BoundingBox box = Objects.requireNonNull(boxGeometry.boundingBox());
            assertVector(box.minimum(), -1.0f, -2.0f, -3.0f);
            assertVector(box.maximum(), 1.0f, 2.0f, 3.0f);
            assertAllTrianglesFaceExpectedNormals(boxGeometry);
        }
    }

    @Test
    void createsUvSphereWithExpectedCountsRadiusAndOutwardWinding() {
        int widthSegments = 8;
        int heightSegments = 4;
        try (BufferGeometry sphere = SphereGeometry.create(2.0f, widthSegments, heightSegments)) {
            assertGeometryShape(
                    sphere, (widthSegments + 1) * (heightSegments + 1), 6 * widthSegments * (heightSegments - 1));
            BoundingSphere bounds = Objects.requireNonNull(sphere.boundingSphere());
            assertVector(bounds.center(), 0.0f, 0.0f, 0.0f);
            assertThat(bounds.radius()).isCloseTo(2.0f, within(JomlAssertions.EPSILON));
            assertAllTrianglesFaceExpectedNormals(sphere);
        }
    }

    @Test
    void createsSphereWithDocumentedDefaultSegments() {
        try (BufferGeometry sphere = SphereGeometry.create(1.0f)) {
            assertThat(sphere.vertexCount()).isEqualTo(33 * 17);
            assertThat(Objects.requireNonNull(sphere.index()).count()).isEqualTo(6 * 32 * 15);
        }
    }

    @Test
    void createsRingWithRadialAngularTextureCoordinatesAndOutwardWinding() {
        try (BufferGeometry ring = RingGeometry.create(1.0f, 3.0f, 4)) {
            assertGeometryShape(ring, 10, 24);
            BoundingBox box = Objects.requireNonNull(ring.boundingBox());
            assertVector(box.minimum(), -3.0f, -3.0f, 0.0f);
            assertVector(box.maximum(), 3.0f, 3.0f, 0.0f);
            BufferAttribute textureCoordinates = Objects.requireNonNull(ring.attribute(BufferGeometry.UV));
            assertThat(textureCoordinates.value(0, 0)).isZero();
            assertThat(textureCoordinates.value(0, 1)).isZero();
            assertThat(textureCoordinates.value(1, 0)).isEqualTo(1.0f);
            assertThat(textureCoordinates.value(1, 1)).isZero();
            assertThat(textureCoordinates.value(8, 0)).isZero();
            assertThat(textureCoordinates.value(8, 1)).isEqualTo(1.0f);
            assertAllTrianglesFaceExpectedNormals(ring);
        }
    }

    @Test
    void createsRingWithDocumentedDefaultSegments() {
        try (BufferGeometry ring = RingGeometry.create(0.5f, 1.0f)) {
            assertThat(ring.vertexCount()).isEqualTo(2 * 33);
            assertThat(Objects.requireNonNull(ring.index()).count()).isEqualTo(6 * 32);
        }
    }

    @Test
    void rejectsInvalidFactoryParameters() {
        assertThatIllegalArgumentException().isThrownBy(() -> PlaneGeometry.create(0.0f, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> BoxGeometry.create(1.0f, Float.NaN, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> SphereGeometry.create(-1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> SphereGeometry.create(1.0f, 2, 4));
        assertThatIllegalArgumentException().isThrownBy(() -> SphereGeometry.create(1.0f, 8, 1));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SphereGeometry.create(1.0f, Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertThatIllegalArgumentException().isThrownBy(() -> RingGeometry.create(-1.0f, 2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> RingGeometry.create(2.0f, 2.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> RingGeometry.create(1.0f, 2.0f, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> RingGeometry.create(1.0f, 2.0f, Integer.MAX_VALUE));
    }

    private static void assertGeometryShape(BufferGeometry geometry, int vertexCount, int indexCount) {
        BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
        BufferAttribute normals = Objects.requireNonNull(geometry.attribute(BufferGeometry.NORMAL));
        BufferAttribute textureCoordinates = Objects.requireNonNull(geometry.attribute(BufferGeometry.UV));
        IndexBuffer index = Objects.requireNonNull(geometry.index());
        assertThat(positions.count()).isEqualTo(vertexCount);
        assertThat(positions.itemSize()).isEqualTo(3);
        assertThat(normals.count()).isEqualTo(vertexCount);
        assertThat(normals.itemSize()).isEqualTo(3);
        assertThat(textureCoordinates.count()).isEqualTo(vertexCount);
        assertThat(textureCoordinates.itemSize()).isEqualTo(2);
        assertThat(index.count()).isEqualTo(indexCount);
        assertThat(positions.version()).isZero();
        assertThat(normals.version()).isZero();
        assertThat(textureCoordinates.version()).isZero();
        assertThat(index.version()).isZero();
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
            assertThat(faceNormal.dot(expectedNormal)).isPositive();
        }
    }

    private static Vector3f position(BufferAttribute positions, int index) {
        return new Vector3f(positions.value(index, 0), positions.value(index, 1), positions.value(index, 2));
    }
}
