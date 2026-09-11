/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.extensions;

import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.extension.EditorExtensions;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Toolkit-independent installed-extension browser consumed by a workbench adapter. */
public final class ExtensionsView implements EditorView {
    /** Stable presentation kind for the bundled extension browser. */
    public static final ViewKindId VIEW_KIND = new ViewKindId("io.github.glynch.jscene3d.editor.extensions-browser");

    private final EditorExtensions extensions;
    private final ExtensionSelection selection;

    ExtensionsView(EditorExtensions extensions, ExtensionSelection selection) {
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        this.selection = Objects.requireNonNull(selection, "selection");
    }

    @Override
    public ViewId id() {
        return ExtensionsExtension.VIEW_ID;
    }

    @Override
    public String title() {
        return "Extensions";
    }

    @Override
    public ViewKindId kind() {
        return VIEW_KIND;
    }

    /**
     * Returns extensions activated in this editor window.
     *
     * @return immutable activated-extension snapshot
     */
    public List<EditorExtensionDescriptor> installed() {
        return extensions.installed();
    }

    /**
     * Observes the complete activated-extension snapshot.
     *
     * @param observer synchronous snapshot observer
     * @return removable observer registration
     */
    public EditorRegistration observe(Consumer<List<EditorExtensionDescriptor>> observer) {
        return extensions.observe(observer);
    }

    /**
     * Returns the selected extension.
     *
     * @return selected extension, or empty
     */
    public Optional<EditorExtensionDescriptor> selection() {
        return selection.current();
    }

    /**
     * Selects an extension and requests its central detail editor.
     *
     * @param descriptor selected extension, or empty
     */
    public void select(Optional<EditorExtensionDescriptor> descriptor) {
        selection.select(descriptor);
    }

    /**
     * Observes extension selection.
     *
     * @param observer synchronous selection observer
     * @return removable observer registration
     */
    public EditorRegistration observeSelection(Consumer<Optional<EditorExtensionDescriptor>> observer) {
        return selection.observe(observer);
    }
}
