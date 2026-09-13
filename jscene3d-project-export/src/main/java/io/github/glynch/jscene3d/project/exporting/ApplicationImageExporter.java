/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.util.Objects;

/** Produces a self-contained native application image from a completed application directory. */
public final class ApplicationImageExporter {
    private final JpackageTool tool;
    private final OperatingSystem operatingSystem;

    /** Uses the {@code jpackage} tool belonging to the current Java runtime on the current host. */
    public ApplicationImageExporter() {
        this(new SystemJpackageTool(), OperatingSystem.current());
    }

    /** Stores replaceable host dependencies for deterministic contract tests. */
    ApplicationImageExporter(JpackageTool tool, OperatingSystem operatingSystem) {
        this.tool = Objects.requireNonNull(tool, "tool");
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
    }

    /**
     * Produces and transactionally installs one native image for the current host.
     *
     * @param request complete application-image request
     * @return installed application image
     * @throws IOException when source inspection, packaging, or installation fails
     * @throws UnsupportedOperationException when the current host is not yet supported
     */
    public ApplicationImage export(ApplicationImageRequest request) throws IOException {
        MacOsApplicationImagePlan plan = MacOsApplicationImagePlan.prepare(request, operatingSystem);
        new MacOsApplicationImageAssembly(plan, tool).write();
        return new ApplicationImage(plan.outputRoot(), plan.outputLauncher());
    }
}
