/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

/** Semantic role used by a platform adapter to present a modal-dialog action. */
public enum EditorDialogButtonRole {
    /** Preferred affirmative action. */
    DEFAULT,
    /** Ordinary alternative action. */
    SECONDARY,
    /** Action which knowingly discards or destroys user state. */
    DESTRUCTIVE,
    /** Action which dismisses the dialog without accepting the operation. */
    CANCEL
}
