/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.io.IOException;
import java.nio.file.Path;

/** Internal seam for creating one macOS disk image from prepared directory content. */
@FunctionalInterface
interface DiskImageTool {
    /**
     * Creates one compressed disk image without opening graphical applications.
     *
     * @param sourceDirectory complete root content of the disk image
     * @param volumeName mounted volume name
     * @param destination exact destination DMG path
     * @throws IOException when disk-image creation fails
     */
    void create(Path sourceDirectory, String volumeName, Path destination) throws IOException;
}
