/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.gui.internal.GuiFont;
import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class GuiFontTest {
    @Test
    void loadsBundledInterAndMeasuresTextAtRequestedSize() {
        GuiFont font = GuiFont.defaultFont();

        assertThat(font.width("Orbit Controls", 14.0f)).isPositive();
        assertThat(font.width("Orbit Controls", 28.0f)).isEqualTo(font.width("Orbit Controls", 14.0f) * 2.0f);
    }

    @Test
    void paintsVisibleAndFallbackGlyphsWhileSkippingWhitespaceBitmap() {
        GuiFont font = GuiFont.defaultFont();
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        font.text(canvas, 10.0f, 20.0f, "A \u2603", 14.0f, Color.WHITE);

        assertThat(canvas.alphaMaskCount()).isEqualTo(2);
        assertThat(font.width("\u2603", 14.0f)).isEqualTo(font.width("?", 14.0f));
    }

    @Test
    void paintsBundledUnicodePunctuationWithoutUsingFallbackGlyph() {
        GuiFont font = GuiFont.defaultFont();
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        font.text(canvas, 10.0f, 20.0f, "\u2014", 14.0f, Color.WHITE);

        assertThat(canvas.alphaMaskCount()).isEqualTo(1);
        assertThat(font.width("\u2014", 14.0f)).isNotEqualTo(font.width("?", 14.0f));
    }
}
