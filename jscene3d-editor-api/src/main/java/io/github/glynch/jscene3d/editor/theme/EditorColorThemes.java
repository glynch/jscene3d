/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.theme;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Registers color themes without exposing JavaFX CSS or a particular source-editor toolkit. */
@FunctionalInterface
public interface EditorColorThemes {
    /**
     * Registers one theme until the returned handle or owning extension is closed.
     *
     * @param theme declarative theme contribution
     * @return removable contribution registration
     */
    EditorRegistration register(EditorColorTheme theme);
}
