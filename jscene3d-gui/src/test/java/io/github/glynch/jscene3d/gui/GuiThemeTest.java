/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.math.Color;
import org.junit.jupiter.api.Test;

final class GuiThemeTest {
    @Test
    void exposesCompleteDarkPaletteAndCustomAccent() {
        GuiTheme theme = GuiTheme.dark(Color.RED);

        assertThat(theme.shadow()).isEqualTo(Color.BLACK);
        assertThat(theme.panel()).isNotNull();
        assertThat(theme.title()).isNotNull();
        assertThat(theme.section()).isNotNull();
        assertThat(theme.row()).isNotNull();
        assertThat(theme.rowHover()).isNotNull();
        assertThat(theme.border()).isNotNull();
        assertThat(theme.text()).isNotNull();
        assertThat(theme.secondaryText()).isNotNull();
        assertThat(theme.mutedText()).isNotNull();
        assertThat(theme.control()).isNotNull();
        assertThat(theme.accent()).isEqualTo(Color.RED);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullAccent() {
        assertThatNullPointerException().isThrownBy(() -> GuiTheme.dark(null));
    }
}
