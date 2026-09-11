/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import java.util.Objects;

/**
 * Places one logical view in a workbench container.
 *
 * @param view toolkit-independent logical view
 * @param container default workbench container
 * @param order ascending presentation order within the container
 */
public record EditorViewContribution(EditorView view, ViewContainerId container, int order) {
    /** Validates one view contribution. */
    public EditorViewContribution {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(view.id(), "view.id()");
        Objects.requireNonNull(view.kind(), "view.kind()");
        String title = Objects.requireNonNull(view.title(), "view.title()");
        if (title.isBlank()) {
            throw new IllegalArgumentException("view title must not be blank");
        }
    }
}
