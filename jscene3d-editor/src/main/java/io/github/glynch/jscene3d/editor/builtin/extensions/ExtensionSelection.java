/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.extensions;

import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Shares extension selection between the browser and central detail editor. */
final class ExtensionSelection {
    private final EditorWindow window;
    private final List<Consumer<Optional<EditorExtensionDescriptor>>> observers = new ArrayList<>();
    private Optional<EditorExtensionDescriptor> selected = Optional.empty();

    ExtensionSelection(EditorWindow window) {
        this.window = Objects.requireNonNull(window, "window");
    }

    Optional<EditorExtensionDescriptor> current() {
        return selected;
    }

    void select(Optional<EditorExtensionDescriptor> descriptor) {
        Optional<EditorExtensionDescriptor> replacement = Objects.requireNonNull(descriptor, "descriptor");
        if (selected.equals(replacement)) {
            if (selected.isPresent()) {
                window.showView(ExtensionsExtension.DETAILS_VIEW_ID);
            }
            return;
        }
        selected = replacement;
        List.copyOf(observers).forEach(observer -> observer.accept(selected));
        if (selected.isPresent()) {
            window.showView(ExtensionsExtension.DETAILS_VIEW_ID);
        }
    }

    EditorRegistration observe(Consumer<Optional<EditorExtensionDescriptor>> observer) {
        Consumer<Optional<EditorExtensionDescriptor>> listener = Objects.requireNonNull(observer, "observer");
        observers.add(listener);
        listener.accept(selected);
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                observers.remove(listener);
            }
        };
    }
}
