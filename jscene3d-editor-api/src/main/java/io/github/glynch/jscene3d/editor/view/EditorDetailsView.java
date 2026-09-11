/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

/** A logical editor view presented as grouped read-only details. */
public interface EditorDetailsView extends EditorView {
    /** Stable kind understood by the standard workbench details adapter. */
    ViewKindId DETAILS_VIEW_KIND = new ViewKindId("io.github.glynch.jscene3d.editor.details");

    /** Returns the standard details view kind. */
    @Override
    default ViewKindId kind() {
        return DETAILS_VIEW_KIND;
    }

    /**
     * Returns the provider for complete details states.
     *
     * @return details data provider
     */
    EditorDetailsDataProvider dataProvider();

    /**
     * Returns the title shown when no details are available.
     *
     * @return empty-state title
     */
    default String emptyTitle() {
        return "Nothing selected";
    }

    /**
     * Returns the guidance shown when no details are available.
     *
     * @return empty-state guidance
     */
    default String emptyMessage() {
        return "Select an item to inspect it.";
    }
}
