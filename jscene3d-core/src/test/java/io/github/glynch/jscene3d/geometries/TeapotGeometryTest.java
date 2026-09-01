/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.geometries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.math.BoundingBox;
import org.junit.jupiter.api.Test;

final class TeapotGeometryTest {
    @Test
    void createsCompleteIndexedTeapotWithSmoothNormalsAndTextureCoordinates() {
        try (BufferGeometry geometry = TeapotGeometry.create(1.0f, 2)) {
            BufferAttribute positions = geometry.attribute(BufferGeometry.POSITION);
            BufferAttribute normals = geometry.attribute(BufferGeometry.NORMAL);
            BufferAttribute textureCoordinates = geometry.attribute(BufferGeometry.UV);

            assertThat(positions).isNotNull();
            assertThat(positions.count()).isEqualTo(288);
            assertThat(normals).isNotNull();
            assertThat(normals.count()).isEqualTo(positions.count());
            assertThat(textureCoordinates).isNotNull();
            assertThat(textureCoordinates.count()).isEqualTo(positions.count());
            assertThat(geometry.index()).isNotNull();
            assertThat(geometry.index().count()).isEqualTo(720);
            assertThat(geometry.boundingBox()).isNotNull();
            assertThat(geometry.boundingSphere()).isNotNull();
            assertThat(geometry.boundingBox().minimum().y()).isEqualTo(-1.0f);
            assertThat(geometry.boundingBox().maximum().y()).isEqualTo(1.0f);
        }
    }

    @Test
    void selectsBodyLidAndBottomPatchesIndependently() {
        try (BufferGeometry body = TeapotGeometry.builder(1.0f)
                        .segments(2)
                        .includeLid(false)
                        .includeBottom(false)
                        .build();
                BufferGeometry lid = TeapotGeometry.builder(1.0f)
                        .segments(2)
                        .includeBody(false)
                        .includeBottom(false)
                        .build();
                BufferGeometry bottom = TeapotGeometry.builder(1.0f)
                        .segments(2)
                        .includeBody(false)
                        .includeLid(false)
                        .build()) {
            assertThat(body.vertexCount()).isEqualTo(180);
            assertThat(body.index().count()).isEqualTo(480);
            assertThat(lid.vertexCount()).isEqualTo(72);
            assertThat(lid.index().count()).isEqualTo(168);
            assertThat(bottom.vertexCount()).isEqualTo(36);
            assertThat(bottom.index().count()).isEqualTo(72);
        }
    }

    @Test
    void fittedLidAndOriginalProportionsChangeTheExpectedDimensions() {
        try (BufferGeometry fittedLid = lid(true);
                BufferGeometry looseLid = lid(false);
                BufferGeometry blinn = TeapotGeometry.create(1.0f, 3);
                BufferGeometry original = TeapotGeometry.builder(1.0f)
                        .segments(3)
                        .blinnProportions(false)
                        .build()) {
            BoundingBox fittedBounds = fittedLid.boundingBox();
            BoundingBox looseBounds = looseLid.boundingBox();
            assertThat(fittedBounds.maximum().x())
                    .isGreaterThan(looseBounds.maximum().x());
            assertThat(blinn.boundingBox().maximum().x())
                    .isGreaterThan(original.boundingBox().maximum().x());
            assertThat(original.boundingBox().minimum().y()).isEqualTo(-1.0f);
            assertThat(original.boundingBox().maximum().y()).isEqualTo(1.0f);
        }
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatIllegalArgumentException().isThrownBy(() -> TeapotGeometry.create(0.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> TeapotGeometry.create(1.0f, 1));
        TeapotGeometry.Builder empty = TeapotGeometry.builder(1.0f)
                .includeBody(false)
                .includeLid(false)
                .includeBottom(false);
        assertThatIllegalArgumentException().isThrownBy(empty::build);
    }

    /** Creates one isolated lid for fitted-versus-loose dimension assertions. */
    private static BufferGeometry lid(boolean fitted) {
        return TeapotGeometry.builder(1.0f)
                .segments(3)
                .includeBody(false)
                .includeBottom(false)
                .fittedLid(fitted)
                .build();
    }
}
