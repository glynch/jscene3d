/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lifecycle;

/** Owns registrations for one activated extension and closes them during deactivation. */
public interface ExtensionSubscriptions {
    /**
     * Adds one registration to the extension lifetime while returning the same handle.
     *
     * @param <T> registration type
     * @param registration registration to own
     * @return the supplied registration
     * @throws IllegalStateException if extension deactivation has begun
     */
    <T extends EditorRegistration> T add(T registration);
}
