/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workingcopy;

import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import java.io.IOException;

/** An editable resource with its own content, dirty state, and persistence lifecycle. */
public interface EditorWorkingCopy {
    /**
     * Returns the stable resource and working-copy type.
     *
     * @return working-copy identity
     */
    EditorWorkingCopyId id();

    /**
     * Returns whether the in-memory content differs from its saved revision.
     *
     * @return whether the resource has unsaved changes
     */
    boolean isDirty();

    /**
     * Returns the event fired after in-memory content changes.
     *
     * @return typed content-change event
     */
    EditorEvent<EditorWorkingCopy> onDidChangeContent();

    /**
     * Returns the event fired only when dirty state changes.
     *
     * @return typed dirty-state event
     */
    EditorEvent<EditorWorkingCopy> onDidChangeDirty();

    /**
     * Returns the event fired after the resource is saved successfully.
     *
     * @return typed save event
     */
    EditorEvent<EditorWorkingCopy> onDidSave();

    /**
     * Persists the current content.
     *
     * @throws IOException when persistence fails
     */
    void save() throws IOException;

    /**
     * Restores the most recently saved content.
     *
     * @throws IOException when restoration requires reading and fails
     */
    void revert() throws IOException;
}
