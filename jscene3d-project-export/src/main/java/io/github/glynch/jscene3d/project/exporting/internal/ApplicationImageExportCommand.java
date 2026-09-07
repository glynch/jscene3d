/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting.internal;

import io.github.glynch.jscene3d.project.exporting.ApplicationImageExporter;
import io.github.glynch.jscene3d.project.exporting.ApplicationImageRequest;
import java.io.IOException;
import java.nio.file.Path;

/** Internal command adapter used by build tools to invoke the native application-image exporter. */
public final class ApplicationImageExportCommand {
    private static final int ARGUMENT_COUNT = 3;

    /** Prevents construction. */
    private ApplicationImageExportCommand() {
        throw new AssertionError("ApplicationImageExportCommand cannot be instantiated");
    }

    /**
     * Exports one application image from ordered build-tool arguments.
     *
     * @param arguments application directory, native version, and output directory
     * @throws IOException when export fails
     */
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                    "expected application directory, application version, and output directory");
        }
        ApplicationImageRequest request = ApplicationImageRequest.builder()
                .applicationDirectory(Path.of(arguments[0]))
                .applicationVersion(arguments[1])
                .outputDirectory(Path.of(arguments[2]))
                .build();
        new ApplicationImageExporter().export(request);
    }
}
