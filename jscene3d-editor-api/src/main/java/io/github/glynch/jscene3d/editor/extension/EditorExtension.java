/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

import java.util.Optional;

/** Trusted executable contribution discovered by the editor at startup. */
public interface EditorExtension {
    /**
     * Returns the stable reverse-domain identity of this extension.
     *
     * @return stable extension identity
     */
    String id();

    /**
     * Returns safe descriptive metadata shown by the editor's extension browser.
     *
     * <p>Extensions distributed outside the editor should override this method with metadata loaded from their
     * package descriptor.
     *
     * @return installed extension metadata
     */
    default EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(), id(), "Installed editor extension", "Unknown publisher", Optional.empty(), false);
    }

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
