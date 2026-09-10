/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.project.runtime.ProjectLaunchRequest;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DesktopProjectRunnerTest {
    @Test
    void marksNamedPlaytestWindow() {
        ProjectLaunchRequest request =
                ProjectLaunchRequest.playtest("moving-floor-34", Path.of("worlds/map01.world.json"), Map.of());

        assertThat(DesktopProjectRunner.windowTitle("Doomed Corridors", request))
                .isEqualTo("Doomed Corridors [PLAYTEST: moving-floor-34]");
        assertThat(DesktopProjectRunner.windowTitle("Doomed Corridors", ProjectLaunchRequest.standard()))
                .isEqualTo("Doomed Corridors");
    }

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
