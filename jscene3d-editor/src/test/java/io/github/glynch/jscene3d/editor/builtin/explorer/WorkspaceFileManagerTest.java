/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class WorkspaceFileManagerTest {
    @Test
    void namesTheNativeFileManagerForEachDesktopPlatform() {
        assertThat(new WorkspaceFileManager("Mac OS X").revealCommandTitle()).isEqualTo("Reveal in Finder");
        assertThat(new WorkspaceFileManager("Windows 11").revealCommandTitle()).isEqualTo("Reveal in File Explorer");
        assertThat(new WorkspaceFileManager("Linux").revealCommandTitle()).isEqualTo("Reveal in File Manager");
    }
}
