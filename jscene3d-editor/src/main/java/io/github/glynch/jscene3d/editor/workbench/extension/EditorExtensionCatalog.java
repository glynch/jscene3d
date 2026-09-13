/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.extension.EditorExtensions;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Owns installed-extension metadata and its ordered observable snapshot. */
final class EditorExtensionCatalog implements EditorExtensions, AutoCloseable {
    private final Map<String, EditorExtensionDescriptor> descriptors = new LinkedHashMap<>();
    private final EditorSnapshotObservers<List<EditorExtensionDescriptor>> observers = new EditorSnapshotObservers<>();
    private boolean closed;

    @Override
    public List<EditorExtensionDescriptor> installed() {
        requireOpen();
        return snapshot();
    }

    @Override
    public EditorRegistration observe(Consumer<List<EditorExtensionDescriptor>> observer) {
        requireOpen();
        return observers.observe(observer, snapshot());
    }

    void add(EditorExtensionDescriptor descriptor) {
        requireOpen();
        EditorExtensionDescriptor registered = Objects.requireNonNull(descriptor, "descriptor");
        if (descriptors.putIfAbsent(registered.id(), registered) != null) {
            throw new IllegalArgumentException("extension identity is already catalogued: " + registered.id());
        }
        observers.publish(snapshot());
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        descriptors.clear();
        observers.publish(snapshot());
        observers.clear();
    }

    private List<EditorExtensionDescriptor> snapshot() {
        return List.copyOf(descriptors.values());
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("extension catalog is closed");
        }
    }
}
