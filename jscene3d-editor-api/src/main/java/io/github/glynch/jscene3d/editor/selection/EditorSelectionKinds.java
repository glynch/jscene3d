/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.selection;

/** Selection kinds published by the built-in JScene3D views. */
public final class EditorSelectionKinds {
    /** A project world definition. */
    public static final EditorSelectionKindId WORLD = kind("world");

    /** An entity authored directly in a world definition. */
    public static final EditorSelectionKindId LOCAL_ENTITY = kind("local-entity");

    /** A placement of an entity definition within a world. */
    public static final EditorSelectionKindId PLACEMENT = kind("placement");

    /** An entity generated from imported or derived project content. */
    public static final EditorSelectionKindId GENERATED_ENTITY = kind("generated-entity");

    /** A project asset. */
    public static final EditorSelectionKindId ASSET = kind("asset");

    private EditorSelectionKinds() {}

    private static EditorSelectionKindId kind(String name) {
        return new EditorSelectionKindId("io.github.glynch.jscene3d.editor." + name);
    }
}
