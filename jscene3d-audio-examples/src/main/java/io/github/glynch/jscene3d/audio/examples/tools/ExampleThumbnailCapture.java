/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio.examples.tools;

import io.github.glynch.jscene3d.audio.examples.ExampleCatalog;
import io.github.glynch.jscene3d.examples.framework.ThumbnailCaptureSupport;
import java.nio.file.Path;
import java.util.List;

/** Refreshes captured thumbnails for the audio example suite. */
public final class ExampleThumbnailCapture {
    private static final String SELECTION_PROPERTY = "jscene3d.thumbnailSelection";
    private static final Path DEFAULT_DIRECTORY =
            Path.of("src", "main", "resources", "META-INF", "jscene3d", "audio-examples", "thumbnails");

    /** Prevents instantiation of this utility entry point. */
    private ExampleThumbnailCapture() {
        throw new AssertionError("ExampleThumbnailCapture cannot be instantiated");
    }

    /**
     * Captures requested definitions, or the complete suite when none are supplied.
     *
     * @param arguments zero or more stable example identifiers
     */
    public static void main(String[] arguments) {
        List<String> exampleIds = arguments.length == 0 ? configuredExampleIds() : List.of(arguments);
        ThumbnailCaptureSupport.capture(ExampleCatalog.suite(), DEFAULT_DIRECTORY, exampleIds);
    }

    /** Reads the Maven-launched comma-separated catalog selection. */
    private static List<String> configuredExampleIds() {
        String selection = System.getProperty(SELECTION_PROPERTY, "");
        return selection.isBlank() ? List.of() : List.of(selection.split(","));
    }
}
