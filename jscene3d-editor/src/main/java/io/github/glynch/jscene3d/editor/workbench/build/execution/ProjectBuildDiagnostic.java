/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.execution;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import java.net.URI;
import java.util.Objects;

/**
 * One structured source diagnostic produced by a project build adapter.
 *
 * @param source absolute source-resource URI
 * @param diagnostic source diagnostic
 */
public record ProjectBuildDiagnostic(URI source, EditorDiagnostic diagnostic) {
    /** Validates one immutable build diagnostic. */
    public ProjectBuildDiagnostic {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(diagnostic, "diagnostic");
    }
}
