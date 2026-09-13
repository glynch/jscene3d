/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import org.junit.jupiter.api.Test;

final class WorkspaceFileManagerTest {
    @Test
    void namesTheNativeFileManagerForEachDesktopPlatform() {
        assertThat(new WorkspaceFileManager(OperatingSystem.MACOS).revealCommandTitle())
                .isEqualTo("Reveal in Finder");
        assertThat(new WorkspaceFileManager(OperatingSystem.WINDOWS).revealCommandTitle())
                .isEqualTo("Reveal in File Explorer");
        assertThat(new WorkspaceFileManager(OperatingSystem.LINUX).revealCommandTitle())
                .isEqualTo("Reveal in File Manager");
        assertThat(new WorkspaceFileManager(OperatingSystem.OTHER).revealCommandTitle())
                .isEqualTo("Reveal in File Manager");
    }
}
