/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

/** Toolkit-independent behavior performed when a modal-dialog button is activated. */
public enum EditorDialogButtonBehavior {
    /** Closes the dialog and returns the button identity to the caller. */
    CLOSE,
    /** Copies the dialog content without closing the dialog. */
    COPY_CONTENT
}
