/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.context;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import org.junit.jupiter.api.Test;

/** Exercises the editor-owned mutable implementation of typed context values. */
final class EditorContextStateTest {
    /** Reports effective changes and exposes values through their declared type. */
    @Test
    void storesTypedContextValues() {
        EditorContextState context = new EditorContextState();

        assertThat(context.get(EditorContextKeys.PROJECT_OPEN)).isEmpty();
        assertThat(context.set(EditorContextKeys.PROJECT_OPEN, false)).isTrue();
        assertThat(context.set(EditorContextKeys.PROJECT_OPEN, false)).isFalse();
        assertThat(context.set(EditorContextKeys.PROJECT_OPEN, true)).isTrue();
        assertThat(context.get(EditorContextKeys.PROJECT_OPEN)).contains(true);
    }
}
