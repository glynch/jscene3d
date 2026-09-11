/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/** Well-known semantic icons supplied by the editor workbench. */
public final class EditorIcons {
    private static final String PREFIX = "io.github.glynch.jscene3d.editor.icon.";

    /** Project or collection root. */
    public static final EditorIconId PROJECT = icon("project");

    /** Authored world definition. */
    public static final EditorIconId WORLD = icon("world");

    /** Local or generated entity. */
    public static final EditorIconId ENTITY = icon("entity");

    /** Reusable entity definition. */
    public static final EditorIconId ENTITY_DEFINITION = icon("entity-definition");

    /** Placement of a reusable entity definition. */
    public static final EditorIconId PLACEMENT = icon("placement");

    /** Source asset owned by the project. */
    public static final EditorIconId SOURCE_ASSET = icon("source-asset");

    /** Import definition. */
    public static final EditorIconId IMPORT = icon("import");

    /** Read-only state. */
    public static final EditorIconId READ_ONLY = icon("read-only");

    /** Initially disabled state. */
    public static final EditorIconId DISABLED = icon("disabled");

    /** Grid presentation. */
    public static final EditorIconId GRID = icon("grid");

    /** List presentation. */
    public static final EditorIconId LIST = icon("list");

    private EditorIcons() {}

    private static EditorIconId icon(String name) {
        return new EditorIconId(PREFIX + name);
    }
}
