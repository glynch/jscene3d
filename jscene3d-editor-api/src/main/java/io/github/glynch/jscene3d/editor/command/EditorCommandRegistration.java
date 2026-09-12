/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Owns a registered command and its mutable presentation state. */
public interface EditorCommandRegistration extends EditorRegistration {
    /** Replaces the command state observed by every visual placement. */
    void update(EditorCommandState state);
}
