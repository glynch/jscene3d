/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.io.IOException;
import java.nio.file.Path;

/** Internal seam for creating one macOS disk image from a validated application image. */
@FunctionalInterface
interface DiskImageTool {
    /**
     * Creates one installable disk image without opening graphical applications.
     *
     * @param plan validated application and branding inputs
     * @param outputDirectory empty directory which receives the generated DMG
     * @param resourceDirectory empty directory for native packaging resources
     * @throws IOException when disk-image creation fails
     */
    void create(MacOsDiskImagePlan plan, Path outputDirectory, Path resourceDirectory) throws IOException;
}
