/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

/** User-facing severity of an editor window message. */
public enum EditorMessageSeverity {
    /** Ordinary informational message. */
    INFORMATION,
    /** Message requiring user attention but not indicating failure. */
    WARNING,
    /** Message reporting a failed operation. */
    ERROR
}
