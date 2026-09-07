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
    private static final int ARGUMENT_COUNT = 2;

    /** Prevents construction. */
    private MacOsDiskImageExportCommand() {
        throw new AssertionError("MacOsDiskImageExportCommand cannot be instantiated");
    }

    /**
     * Exports one macOS disk image from ordered build-tool arguments.
     *
     * @param arguments application image and output directory
     * @throws IOException when export fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != ARGUMENT_COUNT) {
            throw new IllegalArgumentException("expected application image and output directory");
        }
        MacOsDiskImageRequest request = MacOsDiskImageRequest.builder()
                .applicationImage(Path.of(arguments[0]))
                .outputDirectory(Path.of(arguments[1]))
                .build();
        new MacOsDiskImageExporter().export(request);
    }
}
