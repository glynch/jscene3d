/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.selection;

/** Selection kinds published by the built-in JScene3D views. */
public final class EditorSelectionKinds {
    public static final EditorSelectionKindId WORLD = kind("world");
    public static final EditorSelectionKindId LOCAL_ENTITY = kind("local-entity");
    public static final EditorSelectionKindId PLACEMENT = kind("placement");
    public static final EditorSelectionKindId GENERATED_ENTITY = kind("generated-entity");
    public static final EditorSelectionKindId ASSET = kind("asset");

    private EditorSelectionKinds() {}

    private static EditorSelectionKindId kind(String name) {
        return new EditorSelectionKindId("io.github.glynch.jscene3d.editor." + name);
    }
}
