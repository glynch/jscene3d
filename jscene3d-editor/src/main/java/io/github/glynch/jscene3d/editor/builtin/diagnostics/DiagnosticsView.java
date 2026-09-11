/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import java.util.Objects;

/** Toolkit-independent built-in view of extension-published diagnostics. */
public final class DiagnosticsView implements EditorView {
    /** Stable identity of the built-in Diagnostics view. */
    public static final ViewId VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.diagnostics");

    private static final ViewKindId VIEW_KIND_ID = new ViewKindId("io.github.glynch.jscene3d.editor.view.diagnostics");

    private final EditorDiagnosticsModel model;

    /**
     * Creates the view over the shared diagnostics model.
     *
     * @param model shared diagnostics model
     */
    public DiagnosticsView(EditorDiagnosticsModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    @Override
    public ViewId id() {
        return VIEW_ID;
    }

    @Override
    public String title() {
        return "Diagnostics";
    }

    @Override
    public ViewKindId kind() {
        return VIEW_KIND_ID;
    }

    /**
     * Returns the semantic model rendered by the active workbench toolkit.
     *
     * @return shared diagnostics model
     */
    public EditorDiagnosticsModel model() {
        return model;
    }
}
