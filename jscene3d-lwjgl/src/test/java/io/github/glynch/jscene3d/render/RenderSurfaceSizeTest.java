/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

final class RenderSurfaceSizeTest {
    @Test
    void retainsPhysicalAndLogicalDimensions() {
        RenderSurfaceSize size = new RenderSurfaceSize(1920, 1080, 960, 540);

        assertThat(size)
                .returns(1920, RenderSurfaceSize::framebufferWidth)
                .returns(1080, RenderSurfaceSize::framebufferHeight)
                .returns(960, RenderSurfaceSize::logicalWidth)
                .returns(540, RenderSurfaceSize::logicalHeight)
                .returns(true, RenderSurfaceSize::isDrawable);
    }

    @Test
    void representsTemporarilyNonDrawableSurfaces() {
        assertThat(new RenderSurfaceSize(0, 0, 640, 480).isDrawable()).isFalse();
        assertThat(new RenderSurfaceSize(640, 480, 0, 0).isDrawable()).isFalse();
    }

    @Test
    void rejectsNegativeDimensions() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RenderSurfaceSize(-1, 1, 1, 1))
                .withMessage("framebufferWidth must not be negative: -1");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RenderSurfaceSize(1, -1, 1, 1))
                .withMessage("framebufferHeight must not be negative: -1");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RenderSurfaceSize(1, 1, -1, 1))
                .withMessage("logicalWidth must not be negative: -1");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RenderSurfaceSize(1, 1, 1, -1))
                .withMessage("logicalHeight must not be negative: -1");
    }
}
