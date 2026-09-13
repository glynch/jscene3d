/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.lifecycle.ExtensionSubscriptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Owns and reverses the registrations acquired by one active extension. */
final class EditorExtensionSubscriptions implements ExtensionSubscriptions, AutoCloseable {
    private final List<EditorRegistration> registrations = new ArrayList<>();
    private boolean closed;

    @Override
    public <T extends EditorRegistration> T add(T registration) {
        if (closed) {
            throw new IllegalStateException("extension subscriptions are closed");
        }
        T owned = Objects.requireNonNull(registration, "registration");
        registrations.add(owned);
        return owned;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List.copyOf(registrations).reversed().forEach(EditorRegistration::close);
        registrations.clear();
    }
}
