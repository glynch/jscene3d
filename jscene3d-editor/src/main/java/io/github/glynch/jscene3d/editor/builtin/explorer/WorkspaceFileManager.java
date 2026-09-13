/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Provides platform-correct naming and invocation of the desktop file manager. */
final class WorkspaceFileManager {
    private final String operatingSystem;

    WorkspaceFileManager(String operatingSystem) {
        this.operatingSystem =
                Objects.requireNonNull(operatingSystem, "operatingSystem").toLowerCase(Locale.ROOT);
    }

    String revealCommandTitle() {
        if (operatingSystem.contains("mac")) {
            return "Reveal in Finder";
        }
        if (operatingSystem.contains("win")) {
            return "Reveal in File Explorer";
        }
        return "Reveal in File Manager";
    }

    void reveal(Path path) {
        Path target = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException("Desktop integration is unavailable");
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            throw new UnsupportedOperationException("The desktop file manager cannot reveal files");
        }
        desktop.browseFileDirectory(target.toFile());
    }
}
