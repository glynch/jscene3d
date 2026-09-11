/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

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
}
