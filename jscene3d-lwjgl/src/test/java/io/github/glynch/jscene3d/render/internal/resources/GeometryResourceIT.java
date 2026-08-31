/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import io.github.glynch.jscene3d.geometries.BufferAttribute;
import io.github.glynch.jscene3d.geometries.BufferGeometry;
import io.github.glynch.jscene3d.geometries.BufferUsage;
import io.github.glynch.jscene3d.geometries.IndexBuffer;
import io.github.glynch.jscene3d.platform.Window;
import org.junit.jupiter.api.Test;

final class GeometryResourceIT {
    @Test
    void synchronizesChangedReplacedAndRemovedBuffersAcrossEveryUsageHint() {
        try (Window ignored = Window.create("Geometry resource synchronization test");
                BufferGeometry geometry = completeGeometry();
                GeometryResource resource = new GeometryResource()) {
            GeometryResource.UploadResult initialUploads =
                    resource.synchronize(geometry, true, true, true, "Test material");

            assertThat(initialUploads.count()).isEqualTo(5);
            assertThat(initialUploads.byteCount()).isEqualTo(156L);

            assertThat(resource.synchronize(geometry, true, true, true, "Test material")
                            .count())
                    .isZero();

            BufferAttribute replacementPositions = BufferAttribute.of(trianglePositions(), 3, BufferUsage.STREAM);
            geometry.setAttribute(BufferGeometry.POSITION, replacementPositions);
            geometry.setIndex(IndexBuffer.of(new int[] {0, 1, 2}, BufferUsage.DYNAMIC));
            assertThat(resource.synchronize(geometry, true, true, true, "Test material")
                            .count())
                    .isEqualTo(2);

            replacementPositions.setX(0, -0.75f);
            assertThat(resource.synchronize(geometry, true, true, true, "Test material")
                            .count())
                    .isOne();

            geometry.removeAttribute(BufferGeometry.NORMAL);
            geometry.removeAttribute(BufferGeometry.COLOR);
            geometry.removeAttribute(BufferGeometry.UV);
            geometry.clearIndex();
            GeometryResource.UploadResult removalUploads =
                    resource.synchronize(geometry, false, false, false, "Test material");

            assertThat(removalUploads.count()).isZero();
            resource.bind();
        }
    }

    @Test
    void rejectsMissingRequiredAttributes() {
        try (Window ignored = Window.create("Missing geometry attribute test");
                BufferGeometry geometry = new BufferGeometry();
                GeometryResource resource = new GeometryResource()) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, false, false, false))
                    .withMessage("Drawable geometry has no position attribute");

            geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(trianglePositions(), 3));
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, true, false, false))
                    .withMessage("Test material requires a normal attribute but geometry has none");
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, false, true, false))
                    .withMessage("Test material requires a color attribute but geometry has none");
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, false, false, true))
                    .withMessage("Test material requires a uv attribute but geometry has none");
        }
    }

    @Test
    void rejectsIncompatibleOptionalAttributeItemSizes() {
        try (Window ignored = Window.create("Invalid geometry attribute test");
                BufferGeometry geometry = positionedGeometry();
                GeometryResource resource = new GeometryResource()) {
            geometry.setAttribute(BufferGeometry.NORMAL, BufferAttribute.of(new float[6], 2));
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, false, false, false))
                    .withMessage("normal attribute itemSize must be 3: 2");

            geometry.removeAttribute(BufferGeometry.NORMAL);
            geometry.setAttribute(BufferGeometry.COLOR, BufferAttribute.of(new float[6], 2));
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, false, false, false))
                    .withMessage("color attribute itemSize must be 3 or 4: 2");

            geometry.removeAttribute(BufferGeometry.COLOR);
            geometry.setAttribute(BufferGeometry.UV, BufferAttribute.of(new float[9], 3));
            assertThatIllegalStateException()
                    .isThrownBy(() -> synchronize(resource, geometry, false, false, false))
                    .withMessage("uv attribute itemSize must be 2: 3");
        }
    }

    /** Creates geometry containing every renderer-standard buffer. */
    private static BufferGeometry completeGeometry() {
        BufferGeometry geometry = positionedGeometry();
        geometry.setAttribute(
                BufferGeometry.NORMAL,
                BufferAttribute.of(new float[] {0, 0, 1, 0, 0, 1, 0, 0, 1}, 3, BufferUsage.DYNAMIC));
        geometry.setAttribute(
                BufferGeometry.COLOR,
                BufferAttribute.of(new float[] {1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1}, 4, BufferUsage.STREAM));
        geometry.setAttribute(
                BufferGeometry.UV, BufferAttribute.of(new float[] {0, 0, 1, 0, 0, 1}, 2, BufferUsage.STATIC));
        geometry.setIndex(IndexBuffer.of(new int[] {0, 1, 2}, BufferUsage.STREAM));
        return geometry;
    }

    /** Creates a triangle geometry containing only positions. */
    private static BufferGeometry positionedGeometry() {
        BufferGeometry geometry = new BufferGeometry();
        geometry.setAttribute(BufferGeometry.POSITION, BufferAttribute.of(trianglePositions(), 3));
        return geometry;
    }

    /** Returns three counter-clockwise triangle positions. */
    private static float[] trianglePositions() {
        return new float[] {-0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.0f, 0.5f, 0.0f};
    }

    /** Invokes synchronization with the common test material label. */
    private static void synchronize(
            GeometryResource resource,
            BufferGeometry geometry,
            boolean requiresNormals,
            boolean requiresVertexColors,
            boolean requiresTextureCoordinates) {
        resource.synchronize(
                geometry, requiresNormals, requiresVertexColors, requiresTextureCoordinates, "Test material");
    }
}
