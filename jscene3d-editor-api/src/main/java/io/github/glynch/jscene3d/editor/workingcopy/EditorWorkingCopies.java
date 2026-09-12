/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workingcopy;

import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import java.util.List;
import java.util.Optional;

/** Read-only access to the editor's registered resource working copies. */
public interface EditorWorkingCopies {
    /**
     * Finds the working copy with an exact resource identity.
     *
     * @param id resource and working-copy type
     * @return matching working copy, if registered
     */
    Optional<EditorWorkingCopy> find(EditorWorkingCopyId id);

    /**
     * Returns dirty working copies in deterministic registration order.
     *
     * @return immutable dirty working-copy snapshot
     */
    List<EditorWorkingCopy> dirtyWorkingCopies();

    /**
     * Returns whether at least one registered resource is dirty.
     *
     * @return aggregate dirty state
     */
    boolean hasDirty();

    /**
     * Returns the number of dirty registered resources.
     *
     * @return dirty resource count
     */
    int dirtyCount();

    /**
     * Returns the event fired after a working copy is registered.
     *
     * @return typed registration event
     */
    EditorEvent<EditorWorkingCopy> onDidRegister();

    /**
     * Returns the event fired after a working copy is removed.
     *
     * @return typed removal event
     */
    EditorEvent<EditorWorkingCopy> onDidUnregister();

    /**
     * Returns the aggregate event forwarding working-copy content changes.
     *
     * @return typed content-change event
     */
    EditorEvent<EditorWorkingCopy> onDidChangeContent();

    /**
     * Returns the aggregate event forwarding working-copy dirty-state changes.
     *
     * @return typed dirty-state event
     */
    EditorEvent<EditorWorkingCopy> onDidChangeDirty();

    /**
     * Returns the aggregate event forwarding successful saves.
     *
     * @return typed save event
     */
    EditorEvent<EditorWorkingCopy> onDidSave();
}
