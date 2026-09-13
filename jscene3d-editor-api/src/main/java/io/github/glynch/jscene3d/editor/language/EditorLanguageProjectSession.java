/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

/** Owned lifetime of one language adapter within one open project. */
public interface EditorLanguageProjectSession extends AutoCloseable {
    /**
     * Notifies the adapter that a matching text document became open.
     *
     * @param document initial document snapshot
     */
    default void didOpen(EditorTextDocument document) {}

    /**
     * Notifies the adapter that an open document acquired a new version and content.
     *
     * @param change versioned incremental change
     */
    default void didChange(EditorTextDocumentChange change) {}

    /**
     * Notifies the adapter that the current document version was saved successfully.
     *
     * @param document saved document snapshot
     */
    default void didSave(EditorTextDocument document) {}

    /**
     * Notifies the adapter that the final editor for a document closed.
     *
     * @param document final document snapshot
     */
    default void didClose(EditorTextDocument document) {}

    /** Stops the project language session; repeated closure must have no additional effect. */
    @Override
    void close();
}
