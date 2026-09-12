/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

/** Stable identities of commands supplied by the JScene3D editor. */
public final class EditorCommands {
    /** Opens a project directory in the current editor window. */
    public static final CommandId OPEN_PROJECT = new CommandId("io.github.glynch.jscene3d.editor.open-project");

    /** Saves dirty resources in the current project. */
    public static final CommandId SAVE = new CommandId("io.github.glynch.jscene3d.editor.save");

    /** Restores the preceding resource revision. */
    public static final CommandId UNDO = new CommandId("io.github.glynch.jscene3d.editor.undo");

    /** Reapplies the next resource revision. */
    public static final CommandId REDO = new CommandId("io.github.glynch.jscene3d.editor.redo");

    /** Opens the generated settings editor. */
    public static final CommandId OPEN_SETTINGS = new CommandId("io.github.glynch.jscene3d.editor.open-settings");

    /** Shows product and runtime build information. */
    public static final CommandId SHOW_ABOUT = new CommandId("io.github.glynch.jscene3d.editor.show-about");

    /** Requests an orderly editor shutdown. */
    public static final CommandId QUIT = new CommandId("io.github.glynch.jscene3d.editor.quit");

    /** Opens and focuses the editor Diagnostics view. */
    public static final CommandId OPEN_DIAGNOSTICS = new CommandId("io.github.glynch.jscene3d.editor.open-diagnostics");

    private EditorCommands() {}
}
