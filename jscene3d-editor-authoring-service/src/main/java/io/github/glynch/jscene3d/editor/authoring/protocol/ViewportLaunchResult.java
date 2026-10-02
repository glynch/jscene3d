/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.protocol;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Outcome of preparing an isolated viewport launch.
 *
 * @param prepared whether a launch specification was prepared
 * @param launch launch specification on success
 * @param diagnostics ordered world-resolution diagnostics
 * @param failureCode stable operation-level rejection code, when applicable
 */
public record ViewportLaunchResult(
        boolean prepared,
        @Nullable ViewportLaunchSpecification launch,
        List<ProjectDiagnosticDto> diagnostics,
        @Nullable String failureCode) {
    /** Copies diagnostics and validates the mutually exclusive result shapes. */
    public ViewportLaunchResult {
        diagnostics = List.copyOf(diagnostics);
        if ((prepared && (launch == null || failureCode != null)) || (!prepared && launch != null)) {
            throw new IllegalArgumentException("Successful viewport preparation requires a launch specification");
        }
    }
}
