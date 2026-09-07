/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DesktopProjectRunnerTest {
    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void validatesConstructionAndProjectRoot() {
        ClassLoader classLoader = getClass().getClassLoader();
        Path imports = Path.of("imports");

        assertThatThrownBy(() -> new DesktopProjectRunner(null, classLoader, imports))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DesktopProjectRunner("0.1.0", null, imports))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DesktopProjectRunner("0.1.0", classLoader, null))
                .isInstanceOf(NullPointerException.class);
        DesktopProjectRunner runner = new DesktopProjectRunner("0.1.0", classLoader, imports);
        assertThatThrownBy(() -> runner.run(null)).isInstanceOf(NullPointerException.class);
    }
}
