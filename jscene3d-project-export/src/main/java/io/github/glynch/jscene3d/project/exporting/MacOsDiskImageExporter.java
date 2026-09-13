/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.util.Objects;

/** Packages a completed macOS application image as an installable disk image. */
public final class MacOsDiskImageExporter {
    private final DiskImageTool tool;
    private final OperatingSystem operatingSystem;

    /** Uses the non-interactive macOS disk-image tool on the current host. */
    public MacOsDiskImageExporter() {
        this(new SystemDiskImageTool(), OperatingSystem.current());
    }

    /** Stores replaceable host dependencies for deterministic contract tests. */
    MacOsDiskImageExporter(DiskImageTool tool, OperatingSystem operatingSystem) {
        this.tool = Objects.requireNonNull(tool, "tool");
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
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
        MacOsDiskImagePlan plan = MacOsDiskImagePlan.prepare(request, operatingSystem);
        new MacOsDiskImageAssembly(plan, tool).write();
        return new MacOsDiskImage(plan.outputPath());
    }
}
