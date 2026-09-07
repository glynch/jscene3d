/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.project.exporting.internal.ApplicationImageAssembly;
import io.github.glynch.jscene3d.project.exporting.internal.ExportPlan;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Assembles a complete relocatable desktop application image behind one build-tool-independent interface.
 *
 * <p>Export is synchronous and performs filesystem I/O on the calling thread. It validates the project and all
 * inputs before replacing the requested output. Assembly occurs in a sibling staging directory; a failed assembly
 * leaves an existing output image in place whenever the filesystem permits restoration.
 */
public final class ApplicationImageExporter {
    /** Creates an application-image exporter. */
    public ApplicationImageExporter() {
        super();
    }

    /**
     * Validates and exports one application image.
     *
     * @param request complete export request
     * @return paths belonging to the installed application image
     * @throws IllegalArgumentException when the project or an input relationship is invalid
     * @throws IOException when input reading, staging, installation, or cleanup fails
     */
    public ApplicationImage export(ApplicationImageRequest request) throws IOException {
        ExportPlan plan = ExportPlan.prepare(Objects.requireNonNull(request, "request"));
        ApplicationImageAssembly.write(plan);
        Path root = plan.outputDirectory();
        List<Path> artifacts = plan.artifactNames().stream()
                .map(name -> root.resolve("lib").resolve(name))
                .toList();
        return new ApplicationImage(root, plan.launcherName(), artifacts);
    }
}
