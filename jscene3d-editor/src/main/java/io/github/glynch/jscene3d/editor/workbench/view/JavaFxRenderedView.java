/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import java.util.Objects;
import java.util.Optional;
import javafx.scene.Node;

/** Complete JavaFX presentation of one toolkit-independent editor view. */
record JavaFxRenderedView(
        Node node, Node titleGraphic, Optional<Node> titleActions, Runnable close, Runnable requestFocus) {
    JavaFxRenderedView {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(titleGraphic, "titleGraphic");
        Objects.requireNonNull(titleActions, "titleActions");
        Objects.requireNonNull(close, "close");
        Objects.requireNonNull(requestFocus, "requestFocus");
    }
}
