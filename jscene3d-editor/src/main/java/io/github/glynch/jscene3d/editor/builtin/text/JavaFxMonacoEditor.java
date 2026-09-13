/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/** JavaFX WebView adapter hosting Monaco over one text-file working copy. */
public final class JavaFxMonacoEditor implements AutoCloseable {
    private final WebView view = new WebView();
    private final Bridge bridge;
    private boolean closed;

    public JavaFxMonacoEditor(
            EditorTextFileWorkingCopy workingCopy,
            EditorLanguageId language,
            Runnable stateChanged,
            Consumer<String> initializationFailure,
            Consumer<IOException> saveFailure) {
        bridge = new Bridge(workingCopy, language, stateChanged, initializationFailure, saveFailure);
        WebEngine engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((ignored, previous, state) -> {
            if (state == Worker.State.SUCCEEDED && !closed) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaBridge", bridge);
                engine.executeScript("window.attachJavaBridge(javaBridge)");
            }
        });
        engine.load(MonacoResources.editorPage().toExternalForm());
    }

    public Node node() {
        return view;
    }

    public void requestFocus() {
        view.requestFocus();
        execute("window.focusEditor && window.focusEditor()");
    }

    public boolean save() {
        return bridge.save();
    }

    public void undo() {
        execute("window.runEditorCommand && window.runEditorCommand('undo')");
    }

    public void redo() {
        execute("window.runEditorCommand && window.runEditorCommand('redo')");
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            bridge.close();
            execute("window.disposeEditor && window.disposeEditor()");
            view.getEngine().load(null);
        }
    }

    private void execute(String script) {
        if (!closed && view.getEngine().getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            view.getEngine().executeScript(script);
        }
    }

    /** Public bridge methods invoked reflectively by JavaFX WebKit. */
    public static final class Bridge {
        private final EditorTextFileWorkingCopy workingCopy;
        private final EditorLanguageId language;
        private final Runnable stateChanged;
        private final Consumer<String> initializationFailure;
        private final Consumer<IOException> saveFailure;
        private boolean bridgeClosed;

        private Bridge(
                EditorTextFileWorkingCopy workingCopy,
                EditorLanguageId language,
                Runnable stateChanged,
                Consumer<String> initializationFailure,
                Consumer<IOException> saveFailure) {
            this.workingCopy = Objects.requireNonNull(workingCopy, "workingCopy");
            this.language = Objects.requireNonNull(language, "language");
            this.stateChanged = Objects.requireNonNull(stateChanged, "stateChanged");
            this.initializationFailure = Objects.requireNonNull(initializationFailure, "initializationFailure");
            this.saveFailure = Objects.requireNonNull(saveFailure, "saveFailure");
        }

        /** Returns initial source content to JavaScript without source-code interpolation. */
        public String initialContent() {
            return workingCopy.content();
        }

        /** Returns Monaco's language identity. */
        public String language() {
            return language.value();
        }

        /** Returns the canonical resource URI used as Monaco's model identity. */
        public String resource() {
            return workingCopy.id().resource().toString();
        }

        /** Returns the editor's accessible label. */
        public String accessibleLabel() {
            return workingCopy.path().getFileName() + " source editor";
        }

        /** Receives source changes from Monaco. */
        public void contentChanged(String content) {
            if (!bridgeClosed) {
                workingCopy.update(content);
                stateChanged.run();
            }
        }

        /** Reports a JavaScript or resource-loading failure to the workbench. */
        public void initializationFailed(String message) {
            if (!bridgeClosed) {
                initializationFailure.accept(Objects.requireNonNull(message, "message"));
            }
        }

        /** Saves in response to Monaco's platform save shortcut. */
        public void saveRequested() {
            save();
        }

        private boolean save() {
            if (bridgeClosed) {
                return false;
            }
            try {
                workingCopy.save();
                stateChanged.run();
                return true;
            } catch (IOException exception) {
                saveFailure.accept(exception);
                return false;
            }
        }

        private void close() {
            bridgeClosed = true;
        }
    }
}
