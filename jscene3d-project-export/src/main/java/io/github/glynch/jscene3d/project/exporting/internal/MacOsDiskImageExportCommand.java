/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import io.github.glynch.jscene3d.project.exporting.MacOsDiskImageExporter;
import io.github.glynch.jscene3d.project.exporting.MacOsDiskImageRequest;
import java.io.IOException;
import java.nio.file.Path;

/** Internal command adapter used by build tools to invoke the macOS disk-image exporter. */
public final class MacOsDiskImageExportCommand {
    private static final int MINIMUM_ARGUMENT_COUNT = 2;
    private static final int MAXIMUM_ARGUMENT_COUNT = 3;

    /** Prevents construction. */
    private MacOsDiskImageExportCommand() {
        throw new AssertionError("MacOsDiskImageExportCommand cannot be instantiated");
    }

    /**
     * Exports one macOS disk image from ordered build-tool arguments.
     *
     * @param arguments application image, output directory, and optional background image
     * @throws IOException when export fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length < MINIMUM_ARGUMENT_COUNT || arguments.length > MAXIMUM_ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                    "expected application image, output directory, and optional background image");
        }
        MacOsDiskImageRequest.Builder request = MacOsDiskImageRequest.builder()
                .applicationImage(Path.of(arguments[0]))
                .outputDirectory(Path.of(arguments[1]));
        if (arguments.length == MAXIMUM_ARGUMENT_COUNT) {
            request.backgroundImage(Path.of(arguments[2]));
        }
        new MacOsDiskImageExporter().export(request.build());
    }
}
