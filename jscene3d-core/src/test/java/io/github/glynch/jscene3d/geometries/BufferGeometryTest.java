/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static io.github.glynch.jscene3d.testing.JomlAssertions.assertVector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

import io.github.glynch.jscene3d.math.BoundingBox;
import io.github.glynch.jscene3d.math.BoundingSphere;
import io.github.glynch.jscene3d.testing.JomlAssertions;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

final class BufferGeometryTest {
    @Test
    void storesStandardAndCustomAttributesInInsertionOrder() {
        try (BufferGeometry geometry = new BufferGeometry()) {
            BufferAttribute positions = BufferAttribute.of(new float[9], 3);
            BufferAttribute custom = BufferAttribute.of(new float[6], 2);

            geometry.setAttribute(BufferGeometry.POSITION, positions);
            geometry.setAttribute("customData", custom);

            assertThat(geometry.vertexCount()).isEqualTo(3);
            assertThat(geometry.attribute(BufferGeometry.POSITION)).isSameAs(positions);
            assertThat(geometry.attribute("customData")).isSameAs(custom);
            assertThat(geometry.attributes().keySet()).containsExactly(BufferGeometry.POSITION, "customData");
            assertThat(geometry.version()).isEqualTo(2L);

            geometry.setAttribute("customData", custom);
            assertThat(geometry.version()).isEqualTo(2L);
        }
    }

    @Test
    void validatesAttributeShapeAndCounts() {
        try (BufferGeometry geometry = new BufferGeometry()) {
            BufferAttribute invalidPositions = BufferAttribute.of(new float[6], 2);
            BufferAttribute positions = BufferAttribute.of(new float[9], 3);
            BufferAttribute incompatible = BufferAttribute.of(new float[8], 2);

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> geometry.setAttribute(BufferGeometry.POSITION, invalidPositions));
            geometry.setAttribute(BufferGeometry.POSITION, positions);
            assertThatIllegalArgumentException().isThrownBy(() -> geometry.setAttribute("custom", incompatible));
        }
    }

    @Test
    void enforcesSharedIndexValidityAcrossAttachedGeometries() {
        IndexBuffer sharedIndex = IndexBuffer.of(new int[] {0, 1, 1});
        try (BufferGeometry larger = geometryWithVertexCount(3);
                BufferGeometry smaller = geometryWithVertexCount(2)) {
            larger.setIndex(sharedIndex);
            smaller.setIndex(sharedIndex);

            assertThatIllegalArgumentException().isThrownBy(() -> sharedIndex.set(0, 2));

            smaller.clearIndex();
            sharedIndex.set(0, 2);
            assertThat(sharedIndex.value(0)).isEqualTo(2);
        }
    }

    @Test
    void rejectsIndexThatReferencesMissingVertex() {
        try (BufferGeometry geometry = geometryWithVertexCount(3)) {
            IndexBuffer invalidIndex = IndexBuffer.of(new int[] {0, 1, 3});

            assertThatIllegalArgumentException().isThrownBy(() -> geometry.setIndex(invalidIndex));
            assertThat(geometry.index()).isNull();
        }
    }

    @Test
    void managesValidatedIndexedAndNonIndexedDrawRanges() {
        try (BufferGeometry geometry = geometryWithVertexCount(4)) {

            assertThat(geometry.drawRangeStart()).isZero();
            assertThat(geometry.drawRangeCount()).isEqualTo(4);
            geometry.setDrawRange(1, 3);
            assertThat(geometry.hasExplicitDrawRange()).isTrue();
            assertThatIllegalArgumentException().isThrownBy(() -> geometry.setDrawRange(2, 3));

            IndexBuffer shortIndex = IndexBuffer.of(new int[] {0, 1, 2});
            assertThatIllegalArgumentException().isThrownBy(() -> geometry.setIndex(shortIndex));

            geometry.clearDrawRange();
            geometry.setIndex(shortIndex);
            assertThat(geometry.drawRangeCount()).isEqualTo(3);
            assertThatIllegalArgumentException().isThrownBy(() -> geometry.setDrawRange(0, 4));
        }
    }

    @Test
    void computesAndInvalidatesDerivedBounds() {
        try (BufferGeometry geometry = new BufferGeometry()) {
            BufferAttribute positions = BufferAttribute.of(new float[] {-1.0f, 2.0f, 3.0f, 3.0f, -2.0f, 1.0f}, 3);
            geometry.setAttribute(BufferGeometry.POSITION, positions);

            BoundingBox box = geometry.computeBoundingBox();
            BoundingSphere sphere = geometry.computeBoundingSphere();

            assertVector(box.minimum(), -1.0f, -2.0f, 1.0f);
            assertVector(box.maximum(), 3.0f, 2.0f, 3.0f);
            assertVector(sphere.center(), 1.0f, 0.0f, 2.0f);
            assertThat(sphere.radius()).isCloseTo(3.0f, within(JomlAssertions.EPSILON));

            positions.setXYZ(0, -2.0f, 2.0f, 3.0f);
            assertThat(geometry.boundingBox()).isNull();
            assertThat(geometry.boundingSphere()).isNull();
        }
    }

    @Test
    void preservesExplicitBoundsAcrossPositionChanges() {
        try (BufferGeometry geometry = geometryWithVertexCount(1)) {
            BoundingBox box = new BoundingBox(-10.0f, -10.0f, -10.0f, 10.0f, 10.0f, 10.0f);
            BoundingSphere sphere = new BoundingSphere(0.0f, 0.0f, 0.0f, 20.0f);
            geometry.setBoundingBox(box);
            geometry.setBoundingSphere(sphere);
            BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));

            positions.setXYZ(0, 100.0f, 100.0f, 100.0f);

            assertThat(geometry.boundingBox()).isSameAs(box);
            assertThat(geometry.boundingSphere()).isSameAs(sphere);
        }
    }

    @Test
    void computesSmoothIndexedVertexNormals() {
        try (BufferGeometry geometry = new BufferGeometry()) {
            geometry.setAttribute(
                    BufferGeometry.POSITION,
                    BufferAttribute.of(
                            new float[] {
                                0.0f, 0.0f, 0.0f,
                                1.0f, 0.0f, 0.0f,
                                1.0f, 1.0f, 0.0f,
                                0.0f, 1.0f, 0.0f
                            },
                            3));
            geometry.setIndex(IndexBuffer.of(new int[] {0, 1, 2, 0, 2, 3}));

            geometry.computeVertexNormals();

            BufferAttribute normals = Objects.requireNonNull(geometry.attribute(BufferGeometry.NORMAL));
            assertThat(normals.count()).isEqualTo(4);
            for (int vertex = 0; vertex < normals.count(); vertex++) {
                assertThat(normals.value(vertex, 0)).isZero();
                assertThat(normals.value(vertex, 1)).isZero();
                assertThat(normals.value(vertex, 2)).isEqualTo(1.0f);
            }
        }
    }

    @Test
    void rejectsIncompleteTrianglesWhenComputingNormals() {
        try (BufferGeometry geometry = geometryWithVertexCount(2)) {
            assertThatIllegalArgumentException().isThrownBy(geometry::computeVertexNormals);
        }
    }

    @Test
    void closesTerminallyWithoutClosingSharedData() {
        BufferGeometry geometry = geometryWithVertexCount(3);
        BufferAttribute positions = Objects.requireNonNull(geometry.attribute(BufferGeometry.POSITION));
        IndexBuffer index = IndexBuffer.of(new int[] {0, 1, 2});
        geometry.setIndex(index);
        Map<String, BufferAttribute> attributes = geometry.attributes();

        geometry.close();
        geometry.close();

        assertThat(geometry.isClosed()).isTrue();
        assertThat(attributes).isEmpty();
        positions.setXYZ(0, 1.0f, 2.0f, 3.0f);
        index.set(0, 2);
        assertThatIllegalStateException().isThrownBy(geometry::version);
        assertThatIllegalStateException().isThrownBy(geometry::attributes);
    }

    @Test
    void rejectsMissingPositionsForIndexAndBounds() {
        try (BufferGeometry geometry = new BufferGeometry()) {
            IndexBuffer index = IndexBuffer.of(new int[0]);

            assertThatIllegalArgumentException().isThrownBy(() -> geometry.setIndex(index));
            assertThatIllegalStateException().isThrownBy(geometry::computeBoundingBox);
            assertThatIllegalStateException().isThrownBy(geometry::computeBoundingSphere);
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsNullAttributeAndIndexArguments() {
        try (BufferGeometry geometry = new BufferGeometry()) {
            BufferAttribute positions = BufferAttribute.of(new float[3], 3);

            assertThatNullPointerException().isThrownBy(() -> geometry.setAttribute(null, positions));
            assertThatNullPointerException().isThrownBy(() -> geometry.setAttribute(BufferGeometry.POSITION, null));
            assertThatNullPointerException().isThrownBy(() -> geometry.setIndex(null));
        }
    }

    private static BufferGeometry geometryWithVertexCount(int vertexCount) {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(new float[vertexCount * 3], 3));
        return geometry;
    }
}
