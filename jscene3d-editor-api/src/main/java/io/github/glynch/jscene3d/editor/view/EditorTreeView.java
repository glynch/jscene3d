/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.Optional;

/**
 * A logical editor view presented as a lazily populated tree.
 *
 * @param <T> semantic tree-element type
 */
public interface EditorTreeView<T> extends EditorView {
    /** Stable kind understood by the standard workbench tree adapter. */
    ViewKindId TREE_VIEW_KIND = new ViewKindId("io.github.glynch.jscene3d.editor.tree");

    /** Returns the standard tree view kind. */
    @Override
    default ViewKindId kind() {
        return TREE_VIEW_KIND;
    }

    /**
     * Returns the provider for semantic tree elements and their presentations.
     *
     * @return tree data provider
     */
    EditorTreeDataProvider<T> dataProvider();

    /**
     * Returns the optional shared selection model for this tree.
     *
     * <p>The workbench uses this model in both directions: user selection is published to the model, while selection
     * changes originating elsewhere are reflected in the rendered tree.
     *
     * @return shared selection model, or empty when the view does not expose selection
     */
    default Optional<EditorTreeSelectionModel<T>> selectionModel() {
        return Optional.empty();
    }
}
