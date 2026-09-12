/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.configuration.SettingKey;
import org.junit.jupiter.api.Test;

/** Verifies the extension-facing typed configuration-change contract. */
final class EditorConfigurationChangeTest {
    @Test
    void matchesSettingsByStableKey() {
        SettingKey<Boolean> enabled = new SettingKey<>("io.github.glynch.example.enabled", Boolean.class);
        EditorConfigurationChange change = new EditorConfigurationChange(enabled, true);

        assertThat(change.affects(enabled)).isTrue();
        assertThat(change.affects(new SettingKey<>("io.github.glynch.example.mode", String.class)))
                .isFalse();
        assertThat(change.projectOverride()).isTrue();
    }
}
