/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.game.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class InputActionTest {
    @Test
    void behavesAsAStableNamedValue() {
        InputAction first = new InputAction("move-forward");
        InputAction second = new InputAction("move-forward");

        assertThat(first.name()).isEqualTo("move-forward");
        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first.toString()).isEqualTo("move-forward");
        assertThat(first).isNotEqualTo(new InputAction("move-backward"));
    }

    @Test
    void rejectsMissingNames() {
        assertThatThrownBy(() -> new InputAction(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
