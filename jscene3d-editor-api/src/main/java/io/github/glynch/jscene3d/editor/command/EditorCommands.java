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

    /** Switches between the preferred dark and light color-theme families. */
    public static final CommandId TOGGLE_COLOR_SCHEME =
            new CommandId("io.github.glynch.jscene3d.editor.toggle-color-scheme");

    /** Shows product and runtime build information. */
    public static final CommandId SHOW_ABOUT = new CommandId("io.github.glynch.jscene3d.editor.show-about");

    /** Requests an orderly editor shutdown. */
    public static final CommandId QUIT = new CommandId("io.github.glynch.jscene3d.editor.quit");

    /** Opens and focuses the editor Diagnostics view. */
    public static final CommandId OPEN_DIAGNOSTICS = new CommandId("io.github.glynch.jscene3d.editor.open-diagnostics");

    /** Requests an ordinary development build of the open project. */
    public static final CommandId BUILD_PROJECT = new CommandId("io.github.glynch.jscene3d.editor.build-project");

    /** Requests a clean development build of the open project. */
    public static final CommandId REBUILD_PROJECT = new CommandId("io.github.glynch.jscene3d.editor.rebuild-project");

    /** Cancels the build currently running for the open project. */
    public static final CommandId CANCEL_BUILD = new CommandId("io.github.glynch.jscene3d.editor.cancel-build");

    /** Opens the output produced by project builds. */
    public static final CommandId SHOW_BUILD_OUTPUT =
            new CommandId("io.github.glynch.jscene3d.editor.show-build-output");

    /** Toggles automatic builds for the open workspace. */
    public static final CommandId TOGGLE_AUTOMATIC_BUILD =
            new CommandId("io.github.glynch.jscene3d.editor.toggle-automatic-build");

    private EditorCommands() {}
}
