/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.context;

/** Context keys owned by the JScene3D editor. */
public final class EditorContextKeys {
    /** True only while a completely loaded project is published to the editor window. */
    public static final EditorContextKey<Boolean> PROJECT_OPEN =
            new EditorContextKey<>("jscene3d.project-open", Boolean.class);

    private EditorContextKeys() {}
}
