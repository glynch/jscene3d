/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.List;
import java.util.function.Consumer;

/** Read-only catalogue of extensions activated in the current editor window. */
public interface EditorExtensions {
    /**
     * Returns activated extensions in activation order.
     *
     * @return immutable installed-extension snapshot
     */
    List<EditorExtensionDescriptor> installed();

    /**
     * Observes activated extensions.
     *
     * <p>The observer immediately receives the current snapshot.
     *
     * @param observer synchronous installed-extension observer
     * @return removable observer registration
     */
    EditorRegistration observe(Consumer<List<EditorExtensionDescriptor>> observer);
}
