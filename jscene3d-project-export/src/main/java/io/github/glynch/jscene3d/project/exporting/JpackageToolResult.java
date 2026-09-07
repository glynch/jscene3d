/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import java.util.Objects;

/** Captured exit status and merged output from one native-packaging process. */
record JpackageToolResult(int exitCode, String output) {
    /** Validates captured process output. */
    JpackageToolResult {
        Objects.requireNonNull(output, "output");
    }
}
