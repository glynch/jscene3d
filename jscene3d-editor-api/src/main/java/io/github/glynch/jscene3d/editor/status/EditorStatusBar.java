/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.status;

/** Creates extension-owned items presented by the editor status bar. */
@FunctionalInterface
public interface EditorStatusBar {
    /**
     * Creates a hidden status item which remains registered until closed.
     *
     * @param contribution stable identity and placement metadata
     * @return mutable status-item handle
     * @throws IllegalArgumentException if the identity is already registered
     */
    EditorStatusItem create(EditorStatusItemContribution contribution);
}
