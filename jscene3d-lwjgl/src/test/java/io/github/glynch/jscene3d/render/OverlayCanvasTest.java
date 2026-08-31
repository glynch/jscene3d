/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class OverlayCanvasTest {
    @Test
    void accumulatesSolidAndAlphaMaskCommands() {
        OverlayCanvas canvas = new OverlayCanvas();
        OverlayImage image = OverlayImage.alphaMask(1, 1, new byte[] {(byte) 0xff});

        canvas.rectangle(1.0f, 2.0f, 10.0f, 20.0f, Color.RED, 1.0f);
        int rectangleVertexCount = canvas.vertexCount();
        canvas.alphaMask(image.fullRegion(), 5.0f, 6.0f, 10.0f, 12.0f, Color.WHITE, 1.0f);

        assertThat(rectangleVertexCount).isEqualTo(6);
        assertThat(canvas.vertexCount()).isEqualTo(12);
        assertThat(canvas.commandCount()).isEqualTo(2);
        assertThat(canvas.commandImage(0)).isNull();
        assertThat(canvas.commandImage(1)).isSameAs(image);

        canvas.clear();
        assertThat(canvas.vertexCount()).isZero();
    }

    @Test
    void accumulatesFullColorImageCommands() {
        OverlayCanvas canvas = new OverlayCanvas();
        OverlayImage image = OverlayImage.srgbRgba(1, 1, new byte[] {(byte) 0xff, 0, 0, (byte) 0xff});

        canvas.image(image.fullRegion(), 2.0f, 3.0f, 20.0f, 10.0f, Color.WHITE, 0.75f);

        assertThat(canvas.vertexCount()).isEqualTo(6);
        assertThat(canvas.commandCount()).isEqualTo(1);
        assertThat(canvas.commandImage(0)).isSameAs(image);
    }

    @Test
    void tessellatesRoundedRectanglesAndLines() {
        OverlayCanvas canvas = new OverlayCanvas();

        canvas.roundedRectangle(1.0f, 2.0f, 20.0f, 20.0f, 5.0f, Color.BLUE, 1.0f);
        int roundedVertexCount = canvas.vertexCount();
        canvas.line(0.0f, 0.0f, 10.0f, 10.0f, 2.0f, Color.WHITE, 1.0f);

        assertThat(roundedVertexCount).isGreaterThan(6);
        assertThat(canvas.vertexCount()).isEqualTo(roundedVertexCount + 6);
        assertThat(canvas.commandCount()).isEqualTo(1);
    }
}
