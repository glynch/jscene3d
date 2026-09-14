/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies the invariants of enumerated setting choices. */
class SettingChoiceTest {
    @Test
    void acceptsNonBlankValuesAndLabels() {
        SettingChoice choice = new SettingChoice("dark", "Dark theme");

        assertThat(choice.value()).isEqualTo("dark");
        assertThat(choice.label()).isEqualTo("Dark theme");
    }

    @Test
    void rejectsMissingOrBlankValues() {
        assertThatNullPointerException().isThrownBy(() -> new SettingChoice(null, "Dark theme"));
        assertThatThrownBy(() -> new SettingChoice(" ", "Dark theme"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }

    @Test
    void rejectsMissingOrBlankLabels() {
        assertThatNullPointerException().isThrownBy(() -> new SettingChoice("dark", null));
        assertThatThrownBy(() -> new SettingChoice("dark", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label");
    }
}
