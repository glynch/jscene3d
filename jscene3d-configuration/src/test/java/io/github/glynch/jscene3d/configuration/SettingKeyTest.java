/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies setting-key syntax without relying on backtracking regular expressions. */
class SettingKeyTest {
    @Test
    void acceptsLowercaseDottedKeysWithInternalHyphens() {
        SettingKey<Boolean> key = new SettingKey<>("example.editor-test.grid-enabled", Boolean.class);

        assertThat(key.value()).isEqualTo("example.editor-test.grid-enabled");
        assertThat(key.valueClass()).isEqualTo(Boolean.class);
    }

    @Test
    void rejectsMalformedKeys() {
        List<String> malformedKeys = List.of(
                "setting",
                ".setting",
                "setting.",
                "setting..value",
                "setting.1value",
                "Setting.value",
                "setting.-value",
                "setting.value-",
                "setting.some--value",
                "setting.some_value",
                "setting.value\u0661");

        for (String malformedKey : malformedKeys) {
            assertThatThrownBy(() -> new SettingKey<>(malformedKey, String.class))
                    .as("key %s", malformedKey)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lowercase dotted identity");
        }
    }

    @Test
    void rejectsLargeMalformedInputWithoutOverflowingTheStack() {
        String malformedKey = "setting." + "segment.".repeat(100_000);

        assertThatThrownBy(() -> new SettingKey<>(malformedKey, String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase dotted identity");
    }
}
