/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.activity;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Construction-time registry for Activity Bar contributions. */
@FunctionalInterface
public interface EditorActivityRegistry {
    /**
     * Registers one activity for the lifetime of its owning extension.
     *
     * @param contribution activity contribution
     * @return removable registration
     * @throws IllegalArgumentException if another activity has the same identity
     * @throws IllegalStateException if registration has closed
     */
    EditorRegistration register(EditorActivityContribution contribution);
}
