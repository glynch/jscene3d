/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.io.IOException;
import java.util.List;

/** Internal seam between native-image assembly and the external JDK packaging tool. */
@FunctionalInterface
interface JpackageTool {
    /**
     * Executes one complete native-packaging command.
     *
     * @param arguments arguments following the tool executable
     * @return completed process result
     * @throws IOException when the tool cannot be executed or observed
     */
    JpackageToolResult execute(List<String> arguments) throws IOException;
}
