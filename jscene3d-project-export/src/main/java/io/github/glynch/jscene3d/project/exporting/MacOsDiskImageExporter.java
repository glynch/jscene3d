/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.io.IOException;
import java.util.Objects;

/** Packages a completed macOS application image as an installable disk image. */
public final class MacOsDiskImageExporter {
    private final JpackageTool tool;
    private final String operatingSystemName;

    /** Uses the {@code jpackage} tool belonging to the current Java runtime on the current host. */
    public MacOsDiskImageExporter() {
        this(new SystemJpackageTool(), System.getProperty("os.name"));
    }

    /** Stores replaceable host dependencies for deterministic contract tests. */
    MacOsDiskImageExporter(JpackageTool tool, String operatingSystemName) {
        this.tool = Objects.requireNonNull(tool, "tool");
        this.operatingSystemName = Objects.requireNonNull(operatingSystemName, "operatingSystemName");
    }

    /**
     * Packages and transactionally installs one disk image.
     *
     * @param request complete disk-image request
     * @return installed macOS disk image
     * @throws IOException when source inspection, packaging, or installation fails
     * @throws UnsupportedOperationException when the current host is not macOS
     */
    public MacOsDiskImage export(MacOsDiskImageRequest request) throws IOException {
        MacOsDiskImagePlan plan = MacOsDiskImagePlan.prepare(request, operatingSystemName);
        new MacOsDiskImageAssembly(plan, tool).write();
        return new MacOsDiskImage(plan.outputPath());
    }
}
