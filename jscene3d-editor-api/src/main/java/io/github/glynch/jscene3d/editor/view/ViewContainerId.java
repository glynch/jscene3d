/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static io.github.glynch.jscene3d.editor.internal.EditorIdentifiers.requireNamespacedId;

/**
 * Stable identity of a workbench container capable of hosting contributed views.
 *
 * @param value lowercase dotted namespaced identity
 */
public record ViewContainerId(String value) {
    /** Validates one workbench container identity. */
    public ViewContainerId {
        value = requireNamespacedId(value, "value");
    }

    /** Returns the stable serialized value. */
    @Override
    public String toString() {
        return value;
    }
}
