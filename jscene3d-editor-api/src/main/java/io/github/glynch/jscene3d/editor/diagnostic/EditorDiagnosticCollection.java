/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.net.URI;
import java.util.List;

/** Mutable extension-owned diagnostic publication grouped by source URI. */
public interface EditorDiagnosticCollection extends EditorRegistration {
    /**
     * Returns this collection's stable identity.
     *
     * @return stable collection identity
     */
    DiagnosticCollectionId id();

    /**
     * Atomically replaces every diagnostic owned by this collection for one source.
     *
     * @param source source document or resource
     * @param diagnostics immutable-order replacement diagnostics
     * @throws IllegalStateException if this collection has been closed
     */
    void replace(URI source, List<EditorDiagnostic> diagnostics);

    /**
     * Removes every diagnostic owned by this collection for one source.
     *
     * @param source source document or resource
     * @throws IllegalStateException if this collection has been closed
     */
    void clear(URI source);

    /**
     * Removes every diagnostic owned by this collection without closing it.
     *
     * @throws IllegalStateException if this collection has been closed
     */
    void clear();
}
