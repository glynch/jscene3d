/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import java.net.URI;
import java.util.Objects;

/**
 * One diagnostic occurrence published through the extension host.
 *
 * @param collection diagnostic collection that owns the occurrence
 * @param source authoritative source URI
 * @param diagnostic published diagnostic
 */
public record EditorDiagnosticSnapshot(DiagnosticCollectionId collection, URI source, EditorDiagnostic diagnostic) {
    /** Validates one immutable host snapshot entry. */
    public EditorDiagnosticSnapshot {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(diagnostic, "diagnostic");
    }
}
