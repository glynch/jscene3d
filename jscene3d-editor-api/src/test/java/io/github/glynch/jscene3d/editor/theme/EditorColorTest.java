/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies color parsing, formatting, and channel validation. */
final class EditorColorTest {
    @Test
    void roundTripsOpaqueAndTransparentHexColors() {
        assertThat(EditorColor.rgb(1, 2, 3)).hasToString("EditorColor[red=1, green=2, blue=3, alpha=255]");
        assertThat(EditorColor.parseHex("#0a0b0c")).isEqualTo(new EditorColor(10, 11, 12, 255));
        assertThat(EditorColor.parseHex("#0a0b0c7f")).isEqualTo(new EditorColor(10, 11, 12, 127));
        assertThat(EditorColor.rgb(10, 11, 12).toHex()).isEqualTo("#0a0b0c");
        assertThat(new EditorColor(10, 11, 12, 127).toHex()).isEqualTo("#0a0b0c7f");
    }

    @Test
    void rejectsInvalidColorTextAndChannels() {
        assertThatThrownBy(() -> EditorColor.parseHex(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EditorColor.parseHex("#123")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EditorColor.parseHex("123456")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EditorColor.parseHex("#nothex")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EditorColor(-1, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EditorColor(0, 256, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EditorColor(0, 0, 256, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EditorColor(0, 0, 0, 256)).isInstanceOf(IllegalArgumentException.class);
    }
}
