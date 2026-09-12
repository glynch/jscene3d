/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/** Applies a validated textual value from a toolkit adapter to one editable property. */
@FunctionalInterface
public interface EditorPropertyEditor {
    /**
     * Applies the complete replacement value.
     *
     * @param value replacement value in the property's serialized form
     */
    void setValue(String value);
}
