/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class GuiFontTest {
    @Test
    void loadsBundledInterAndMeasuresTextAtRequestedSize() {
        GuiFont font = GuiFont.defaultFont();

        assertThat(font.width("Orbit Controls", 14.0f)).isPositive();
        assertThat(font.width("Orbit Controls", 28.0f)).isEqualTo(font.width("Orbit Controls", 14.0f) * 2.0f);
    }
}
