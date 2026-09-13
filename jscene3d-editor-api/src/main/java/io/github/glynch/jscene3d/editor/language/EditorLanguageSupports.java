/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;

/** Registers project-scoped language adapters without exposing their implementation technology. */
@FunctionalInterface
public interface EditorLanguageSupports {
    /**
     * Registers one language adapter for the activating extension's lifetime.
     *
     * @param contribution stable identity and handled languages
     * @param support project-scoped language implementation
     * @return removable registration
     * @throws IllegalArgumentException if the identity or any language is already registered
     */
    EditorRegistration register(EditorLanguageSupportContribution contribution, EditorLanguageSupport support);
}
