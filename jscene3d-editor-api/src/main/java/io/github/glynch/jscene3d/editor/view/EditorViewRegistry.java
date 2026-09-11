/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/** Construction-time registry for editor view contributions. */
@FunctionalInterface
public interface EditorViewRegistry {
    /**
     * Registers one logical view and its default workbench placement.
     *
     * @param contribution view contribution
     * @throws IllegalArgumentException if another view has the same identity
     * @throws IllegalStateException if registration has closed
     */
    void register(EditorViewContribution contribution);
}
