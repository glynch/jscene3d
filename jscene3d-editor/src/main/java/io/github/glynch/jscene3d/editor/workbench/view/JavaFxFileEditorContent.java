/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import java.util.Objects;
import javafx.scene.Node;

/** JavaFX content and editor operations hosted by one file tab. */
record JavaFxFileEditorContent(Node node, Runnable focus, Runnable undo, Runnable redo, AutoCloseable close) {
    JavaFxFileEditorContent {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(undo, "undo");
        Objects.requireNonNull(redo, "redo");
        Objects.requireNonNull(close, "close");
    }
}
