/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.window.EditorWindow;

/** Invocation-scoped facilities supplied to an executing editor command. */
public interface EditorCommandContext {
    /**
     * Returns safe interaction with the containing editor window.
     *
     * @return editor window facility
     */
    EditorWindow window();
}
