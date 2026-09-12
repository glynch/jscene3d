/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import java.util.Objects;
import java.util.Optional;

/**
 * Places one logical view in a workbench container.
 *
 * @param view toolkit-independent logical view
 * @param container default workbench container
 * @param order ascending presentation order within the container
 * @param condition optional context condition controlling availability
 */
public record EditorViewContribution(
        EditorView view, ViewContainerId container, int order, Optional<EditorContextCondition<?>> condition) {
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
        Objects.requireNonNull(condition, "condition");
    }

    /** Creates an unconditionally available view contribution. */
    public EditorViewContribution(EditorView view, ViewContainerId container, int order) {
        this(view, container, order, Optional.empty());
    }

    /** Creates a view contribution controlled by one typed context condition. */
    public EditorViewContribution(
            EditorView view, ViewContainerId container, int order, EditorContextCondition<?> condition) {
        this(view, container, order, Optional.of(Objects.requireNonNull(condition, "condition")));
    }
}
