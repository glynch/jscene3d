/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Verifies deterministic host operating-system classification. */
final class OperatingSystemTest {
    @Test
    void recognizesSupportedOperatingSystemFamilies() {
        assertThat(OperatingSystem.fromName("Mac OS X")).isEqualTo(OperatingSystem.MACOS);
        assertThat(OperatingSystem.fromName("macOS")).isEqualTo(OperatingSystem.MACOS);
        assertThat(OperatingSystem.fromName("Darwin")).isEqualTo(OperatingSystem.MACOS);
        assertThat(OperatingSystem.fromName("Windows 11")).isEqualTo(OperatingSystem.WINDOWS);
        assertThat(OperatingSystem.fromName("Linux")).isEqualTo(OperatingSystem.LINUX);
    }

    @Test
    void treatsUnknownAndBlankNamesAsOther() {
        assertThat(OperatingSystem.fromName("FreeBSD")).isEqualTo(OperatingSystem.OTHER);
        assertThat(OperatingSystem.fromName("  ")).isEqualTo(OperatingSystem.OTHER);
    }

    @Test
    void rejectsANameThatIsNotPresent() {
        assertThatThrownBy(() -> OperatingSystem.fromName(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void reportsTheCurrentHostThroughTheSameClassifier() {
        assertThat(OperatingSystem.current()).isEqualTo(OperatingSystem.fromName(System.getProperty("os.name", "")));
    }
}
