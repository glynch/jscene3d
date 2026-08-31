/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.tools;

import io.github.glynch.jscene3d.examples.ThumbnailCaptureSupport;
import java.nio.file.Path;

/** Command-line developer utility that refreshes example-browser thumbnail images. */
public final class ExampleThumbnailCapture {
    private static final Path DEFAULT_DIRECTORY =
            Path.of("src", "main", "resources", "io", "github", "glynch", "jscene3d", "examples", "thumbnails");

    /** Prevents instantiation of this utility entry point. */
    private ExampleThumbnailCapture() {
        throw new AssertionError("ExampleThumbnailCapture cannot be instantiated");
    }

    /**
     * Renders all catalogue entries into the optional output directory.
     *
     * @param arguments zero arguments for the source-resource directory, or one explicit directory
     */
    public static void main(String[] arguments) {
        if (arguments.length > 1) {
            throw new IllegalArgumentException("Expected zero or one thumbnail output directory argument");
        }
        Path destination = arguments.length == 0 ? DEFAULT_DIRECTORY : Path.of(arguments[0]);
        ThumbnailCaptureSupport.capture(destination);
    }
}
