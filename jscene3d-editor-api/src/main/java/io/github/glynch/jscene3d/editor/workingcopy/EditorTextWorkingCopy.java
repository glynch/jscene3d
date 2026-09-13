/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workingcopy;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.language.EditorTextDocumentChange;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;

/** Text working copy whose current content can be synchronized with language tooling. */
public interface EditorTextWorkingCopy extends EditorWorkingCopy {
    /**
     * Returns the language used to interpret the text.
     *
     * @return editor language identity
     */
    EditorLanguageId language();

    /**
     * Returns the monotonically increasing in-memory document version.
     *
     * @return positive current version
     */
    int version();

    /**
     * Returns the current authoritative in-memory text.
     *
     * @return current text
     */
    String content();

    /**
     * Returns detailed, ordered text changes suitable for incremental language synchronization.
     *
     * @return typed incremental-change event
     */
    EditorEvent<EditorTextDocumentChange> onDidChangeText();
}
