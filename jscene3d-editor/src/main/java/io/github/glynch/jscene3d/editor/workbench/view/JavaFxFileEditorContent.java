/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.Node;

/** JavaFX content and editor operations hosted by one file tab. */
record JavaFxFileEditorContent(
        Node node,
        Runnable focus,
        Runnable undo,
        Runnable redo,
        Consumer<EditorTextRange> reveal,
        AutoCloseable close) {
    JavaFxFileEditorContent {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(focus, "focus");
        Objects.requireNonNull(undo, "undo");
        Objects.requireNonNull(redo, "redo");
        Objects.requireNonNull(reveal, "reveal");
        Objects.requireNonNull(close, "close");
    }
}
