/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.diagnostic;

/** Creates extension-owned diagnostic collections aggregated by the editor. */
@FunctionalInterface
public interface EditorDiagnostics {
    /**
     * Creates an initially empty diagnostic collection.
     *
     * @param id stable collection identity
     * @return mutable diagnostic collection
     * @throws IllegalArgumentException if the identity is already registered
     */
    EditorDiagnosticCollection createCollection(DiagnosticCollectionId id);
}
