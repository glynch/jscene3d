/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.nio.file.Path;
import java.util.Objects;

/** Immutable description of one macOS disk image produced from an application image. */
public final class MacOsDiskImage {
    private final Path path;

    /** Stores the path validated by the disk-image exporter. */
    MacOsDiskImage(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    /**
     * Returns the exported disk image.
     *
     * @return normalized absolute DMG path
     */
    public Path path() {
        return path;
    }
}
