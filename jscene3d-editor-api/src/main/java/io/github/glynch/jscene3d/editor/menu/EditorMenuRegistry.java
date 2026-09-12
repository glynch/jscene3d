/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.menu;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Registers extension-owned top-level menu declarations. */
@FunctionalInterface
public interface EditorMenuRegistry {
    /**
     * Registers a top-level menu until the returned registration closes.
     *
     * @param menu menu declaration
     * @return removable menu registration
     */
    EditorRegistration register(EditorMenuContribution menu);
}
