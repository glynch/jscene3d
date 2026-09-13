/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.awt.Desktop;
import java.nio.file.Path;
import java.util.Objects;

/** Provides platform-correct naming and invocation of the desktop file manager. */
final class WorkspaceFileManager {
    private final OperatingSystem operatingSystem;

    WorkspaceFileManager(OperatingSystem operatingSystem) {
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
    }

    String revealCommandTitle() {
        return switch (operatingSystem) {
            case MACOS -> "Reveal in Finder";
            case WINDOWS -> "Reveal in File Explorer";
            case LINUX, OTHER -> "Reveal in File Manager";
        };
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
