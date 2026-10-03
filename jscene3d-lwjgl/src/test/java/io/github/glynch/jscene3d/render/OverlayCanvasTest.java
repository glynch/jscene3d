/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.data.Offset.offset;

import io.github.glynch.jscene3d.math.Color;
import java.util.Objects;
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

    @Test
    void accumulatesSolidTriangles() {
        OverlayCanvas canvas = new OverlayCanvas();

        canvas.triangle(new OverlayCanvas.Triangle(1.0f, 2.0f, 8.0f, 3.0f, 4.0f, 10.0f), Color.GREEN, 0.8f);

        assertThat(canvas.vertexCount()).isEqualTo(3);
        assertThat(canvas.commandCount()).isEqualTo(1);
        assertThat(canvas.commandImage(0)).isNull();
    }

    @Test
    void drawsAntialiasedTextWithRequestedColorAlphaAndLogicalSize() {
        OverlayCanvas canvas = new OverlayCanvas();

        canvas.text("Axis", 20.0f, 30.0f, 24.0f, Color.GREEN, 0.65f);

        assertThat(canvas.vertexCount()).isPositive();
        assertThat(canvas.commandCount()).isEqualTo(1);
        OverlayImage atlas = Objects.requireNonNull(canvas.commandImage(0));
        assertThat(atlas.format()).isEqualTo(OverlayImageFormat.ALPHA_MASK);
        assertThat(canvas.vertices()[4]).isEqualTo(Color.GREEN.red());
        assertThat(canvas.vertices()[5]).isEqualTo(Color.GREEN.green());
        assertThat(canvas.vertices()[6]).isEqualTo(Color.GREEN.blue());
        assertThat(canvas.vertices()[7]).isEqualTo(0.65f);
        assertThat(canvas.textWidth("Axis", 48.0f))
                .isCloseTo(canvas.textWidth("Axis", 24.0f) * 2.0f, offset(0.000_01f));
    }

    @Test
    void alignsTextPredictablyAroundTheLogicalAnchor() {
        OverlayCanvas canvas = new OverlayCanvas();
        float width = canvas.textWidth("XYZ", 20.0f);

        canvas.text("XYZ", 100.0f, 10.0f, 20.0f, Color.WHITE, 1.0f, OverlayTextAlignment.LEFT);
        float leftOrigin = minimumVertexX(canvas);
        canvas.clear();
        canvas.text("XYZ", 100.0f, 10.0f, 20.0f, Color.WHITE, 1.0f, OverlayTextAlignment.CENTER);
        float centerOrigin = minimumVertexX(canvas);
        canvas.clear();
        canvas.text("XYZ", 100.0f, 10.0f, 20.0f, Color.WHITE, 1.0f, OverlayTextAlignment.RIGHT);
        float rightOrigin = minimumVertexX(canvas);

        assertThat(leftOrigin - centerOrigin).isCloseTo(width * 0.5f, offset(0.000_1f));
        assertThat(leftOrigin - rightOrigin).isCloseTo(width, offset(0.000_1f));
    }

    @Test
    void acceptsUnicodeCodePointsAndUsesFallbackForUnavailableGlyphs() {
        OverlayCanvas canvas = new OverlayCanvas();

        canvas.text("Café — 🌐", 0.0f, 0.0f, 16.0f, Color.WHITE, 1.0f);

        assertThat(canvas.vertexCount()).isPositive();
        assertThat(canvas.textWidth("🌐", 16.0f)).isEqualTo(canvas.textWidth("?", 16.0f));
        assertThat(canvas.textWidth("—", 16.0f)).isNotEqualTo(canvas.textWidth("?", 16.0f));
    }

    @Test
    void clearsTextCommandsWithoutRetainingAVisibleFrame() {
        OverlayCanvas canvas = new OverlayCanvas();
        canvas.text("X", 0.0f, 0.0f, 16.0f, Color.WHITE, 1.0f);

        canvas.clear();

        assertThat(canvas.vertexCount()).isZero();
        assertThat(canvas.commandCount()).isZero();
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsInvalidTextArguments() {
        OverlayCanvas canvas = new OverlayCanvas();

        assertThatNullPointerException().isThrownBy(() -> canvas.text(null, 0.0f, 0.0f, 16.0f, Color.WHITE, 1.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> canvas.text("two\nlines", 0.0f, 0.0f, 16.0f, Color.WHITE, 1.0f));
        assertThatIllegalArgumentException().isThrownBy(() -> canvas.text("X", 0.0f, 0.0f, 0.0f, Color.WHITE, 1.0f));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> canvas.text("X", Float.NaN, 0.0f, 16.0f, Color.WHITE, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> canvas.text("X", 0.0f, 0.0f, 16.0f, Color.WHITE, 1.0f, null));
    }

    /** Returns the least logical x-coordinate in the accumulated vertex buffer. */
    private static float minimumVertexX(OverlayCanvas canvas) {
        float minimum = Float.POSITIVE_INFINITY;
        for (int vertex = 0; vertex < canvas.vertexCount(); vertex++) {
            minimum = Math.min(minimum, canvas.vertices()[vertex * 8]);
        }
        return minimum;
    }
}
