/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import java.nio.file.Path;
import java.util.Objects;

/** One validated source file and its portable relative destination. */
record ExportFile(Path source, Path destination) {
    /** Validates one planned copy. */
    ExportFile {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        if (destination.isAbsolute() || destination.getNameCount() == 0) {
            throw new IllegalArgumentException("destination must be a non-empty relative path: " + destination);
        }
    }
}
