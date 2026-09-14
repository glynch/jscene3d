/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

/** Determines how a command's state is presented on visual command surfaces. */
public enum EditorCommandKind {
    /** Ordinary momentary command. */
    ACTION,

    /** Command whose selected state is presented as a checkable choice. */
    TOGGLE
}
