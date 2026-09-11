/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.command;

import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.Objects;

/** Well-known editor-owned surfaces on which extensions can place commands. */
public final class EditorCommandLocations {
    /** Commands displayed in the editor's File menu. */
    public static final CommandLocationId FILE_MENU =
            new CommandLocationId("io.github.glynch.jscene3d.editor.file-menu");

    /** Commands displayed in the viewport-local toolbar. */
    public static final CommandLocationId VIEWPORT_TOOLBAR =
            new CommandLocationId("io.github.glynch.jscene3d.editor.viewport-toolbar");

    private EditorCommandLocations() {}

    /**
     * Returns the title-bar command location for one contributed view.
     *
     * @param view target view
     * @return view title-bar command location
     */
    public static CommandLocationId viewTitle(ViewId view) {
        Objects.requireNonNull(view, "view");
        return viewLocation(view, "title");
    }

    /**
     * Returns the item context-menu command location for one contributed view.
     *
     * @param view target view
     * @return view item context-menu command location
     */
    public static CommandLocationId viewItemContext(ViewId view) {
        Objects.requireNonNull(view, "view");
        return viewLocation(view, "item-context");
    }

    private static CommandLocationId viewLocation(ViewId view, String surface) {
        return new CommandLocationId(view.value() + ".command-location." + surface);
    }
}
