/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import java.nio.file.Path;
import java.util.Objects;

/** One project-contained filesystem entry presented by the Workspace Explorer. */
record WorkspaceExplorerEntry(Path path, String label, Kind kind) {
    /** Validates one normalized absolute workspace entry. */
    WorkspaceExplorerEntry {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        if (Objects.requireNonNull(label, "label").isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        Objects.requireNonNull(kind, "kind");
    }

    /** Distinguishes the workspace root, directories, and ordinary files. */
    enum Kind {
        WORKSPACE,
        DIRECTORY,
        FILE
    }
}
