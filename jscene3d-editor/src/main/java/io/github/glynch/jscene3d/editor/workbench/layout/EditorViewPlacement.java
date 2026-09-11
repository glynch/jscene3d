/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import java.util.Objects;

/**
 * Resolved workbench placement of one contributed view.
 *
 * @param view toolkit-independent logical view
 * @param container current workbench container
 * @param order ascending presentation order within the container
 * @param movable whether the user may move the view to another workbench container
 */
public record EditorViewPlacement(EditorView view, ViewContainerId container, int order, boolean movable) {
    /** Validates one resolved placement. */
    public EditorViewPlacement {
        Objects.requireNonNull(view, "view");
        Objects.requireNonNull(container, "container");
        Objects.requireNonNull(view.id(), "view.id()");
    }
}
