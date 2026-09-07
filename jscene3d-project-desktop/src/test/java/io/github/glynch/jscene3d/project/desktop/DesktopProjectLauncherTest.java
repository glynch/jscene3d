/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class DesktopProjectLauncherTest {
    @Test
    void rejectsIncompleteLaunchConfigurationBeforeAccessingNativeState() {
        String[] noArguments = {};
        String[] incompleteArguments = {"0.1.0", "project"};

        assertThatThrownBy(() -> DesktopProjectLauncher.main(noArguments))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("engine-version")
                .hasMessageContaining("project-directory")
                .hasMessageContaining("published-content-directory");
        assertThatThrownBy(() -> DesktopProjectLauncher.main(incompleteArguments))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
