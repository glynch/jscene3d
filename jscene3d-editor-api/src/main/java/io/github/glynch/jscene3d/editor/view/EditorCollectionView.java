/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.List;
import java.util.Optional;

/**
 * A logical editor view presented as a searchable grid or list with optional category navigation.
 *
 * @param <T> semantic collection-element type
 */
public interface EditorCollectionView<T> extends EditorView {
    /** Stable kind understood by the standard workbench collection adapter. */
    ViewKindId COLLECTION_VIEW_KIND = new ViewKindId("io.github.glynch.jscene3d.editor.collection");

    /** Returns the standard collection view kind. */
    @Override
    default ViewKindId kind() {
        return COLLECTION_VIEW_KIND;
    }

    /**
     * Returns the provider for semantic elements and their presentations.
     *
     * @return collection data provider
     */
    EditorCollectionDataProvider<T> dataProvider();

    /**
     * Returns categories in navigation order; an empty list suppresses category navigation.
     *
     * @return immutable category list
     */
    default List<EditorCollectionCategory> categories() {
        return List.of();
    }

    /**
     * Returns the breadcrumb label for the unfiltered root location.
     *
     * @return root breadcrumb label
     */
    default String allItemsLabel() {
        return "All Items";
    }

    /**
     * Returns the optional editor icon presentation for the collection root.
     *
     * @return root icon, or empty when the root has no icon
     */
    default Optional<EditorIcon> rootIcon() {
        return Optional.empty();
    }

    /**
     * Returns the prompt shown by the workbench's local search field.
     *
     * @return search-field prompt
     */
    default String searchPlaceholder() {
        return "Search items…";
    }

    /**
     * Returns the optional shared selection model for this collection.
     *
     * @return shared selection model, or empty when selection is local to the view
     */
    default Optional<EditorCollectionSelectionModel<T>> selectionModel() {
        return Optional.empty();
    }
}
