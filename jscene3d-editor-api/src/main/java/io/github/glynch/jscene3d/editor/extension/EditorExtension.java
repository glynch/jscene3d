/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

/** Trusted executable contribution discovered by the editor at startup. */
public interface EditorExtension {
    /**
     * Returns the stable reverse-domain identity of this extension.
     *
     * @return stable extension identity
     */
    String id();

    /**
     * Contributes editor facilities during startup.
     *
     * <p>The supplied context remains valid for the extension's activated lifetime. Implementations must not construct
     * toolkit controls and must add retained registrations to the context's subscriptions.
     *
     * @param context bounded editor-owned registration context
     */
    void activate(EditorExtensionContext context);
}
