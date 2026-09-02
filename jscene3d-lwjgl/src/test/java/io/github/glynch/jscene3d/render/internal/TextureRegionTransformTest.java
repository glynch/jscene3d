/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.textures.TextureRegion;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class TextureRegionTransformTest {
    @Test
    void mapsTheUnitSquareIntoTheSelectedRegion() {
        Matrix3f matrix = TextureRegionTransform.apply(new Matrix3f(), new TextureRegion(0.25f, 0.5f, 0.5f, 0.25f));

        assertThat(matrix.transform(new Vector3f(0.0f, 0.0f, 1.0f))).isEqualTo(new Vector3f(0.25f, 0.5f, 1.0f));
        assertThat(matrix.transform(new Vector3f(1.0f, 1.0f, 1.0f))).isEqualTo(new Vector3f(0.75f, 0.75f, 1.0f));
    }

    @Test
    void appliesTheRegionAfterTheExistingTextureTransform() {
        Matrix3f matrix = new Matrix3f().set(0.5f, 0.0f, 0.0f, 0.0f, 0.25f, 0.0f, 0.1f, 0.2f, 1.0f);
        Vector3f transformedBeforeRegion = matrix.transform(new Vector3f(0.4f, 0.8f, 1.0f), new Vector3f());
        TextureRegion region = new TextureRegion(0.25f, 0.5f, 0.5f, 0.25f);

        TextureRegionTransform.apply(matrix, region);

        Vector3f expected = new Vector3f(
                region.u() + region.width() * transformedBeforeRegion.x,
                region.v() + region.height() * transformedBeforeRegion.y,
                1.0f);
        assertThat(matrix.transform(new Vector3f(0.4f, 0.8f, 1.0f))).isEqualTo(expected);
    }
}
