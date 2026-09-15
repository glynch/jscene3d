/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.output.EditorOutputChannel;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

/** JavaFX presentation of one toolkit-independent output channel. */
final class JavaFxOutputChannelAdapter implements AutoCloseable {
    private final TextArea output = new TextArea();
    private final EditorRegistration observation;
    private boolean closed;

    JavaFxOutputChannelAdapter(EditorOutputChannel channel) {
        EditorOutputChannel source = Objects.requireNonNull(channel, "channel");
        output.setEditable(false);
        output.setWrapText(false);
        output.setAccessibleText(source.title() + " output");
        output.getStyleClass().add(EditorStyleClasses.EDITOR_OUTPUT_CHANNEL);
        observation = source.observe(this::show);
    }

    TextArea node() {
        return output;
    }

    void requestFocus() {
        output.requestFocus();
    }

    @Override
    public void close() {
        closed = true;
        observation.close();
    }

    private void show(String content) {
        if (Platform.isFxApplicationThread()) {
            replace(content);
        } else {
            Platform.runLater(() -> replace(content));
        }
    }

    private void replace(String content) {
        if (closed) {
            return;
        }
        output.setText(content);
        output.positionCaret(content.length());
    }
}
