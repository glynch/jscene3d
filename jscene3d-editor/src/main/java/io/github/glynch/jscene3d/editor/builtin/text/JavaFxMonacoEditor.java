/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.appearance.EditorAppearanceSnapshot;
import io.github.glynch.jscene3d.editor.workbench.appearance.EditorColorThemeRegistry;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/** JavaFX WebView adapter hosting Monaco over one text-file working copy. */
public final class JavaFxMonacoEditor implements AutoCloseable {
    private final WebView view = new WebView();
    private final Bridge bridge;
    private final EditorRegistration appearanceRegistration;
    private final EditorRegistration diagnosticRegistration;
    private boolean closed;

    public JavaFxMonacoEditor(
            EditorTextFileWorkingCopy workingCopy,
            EditorLanguageId language,
            EditorExtensionHost extensions,
            Runnable stateChanged,
            Consumer<String> initializationFailure,
            Consumer<IOException> saveFailure) {
        this(
                workingCopy,
                language,
                requireHost(extensions).colorThemes(),
                observer -> requireHost(extensions)
                        .observeDiagnostics(workingCopy.id().resource(), observer),
                stateChanged,
                initializationFailure,
                saveFailure);
    }

    JavaFxMonacoEditor(
            EditorTextFileWorkingCopy workingCopy,
            EditorLanguageId language,
            EditorColorThemeRegistry appearances,
            Runnable stateChanged,
            Consumer<String> initializationFailure,
            Consumer<IOException> saveFailure) {
        this(workingCopy, language, appearances, ignored -> () -> {}, stateChanged, initializationFailure, saveFailure);
    }

    private JavaFxMonacoEditor(
            EditorTextFileWorkingCopy workingCopy,
            EditorLanguageId language,
            EditorColorThemeRegistry appearances,
            DiagnosticObserver diagnosticObserver,
            Runnable stateChanged,
            Consumer<String> initializationFailure,
            Consumer<IOException> saveFailure) {
        bridge = new Bridge(workingCopy, language, stateChanged, initializationFailure, saveFailure);
        appearanceRegistration = Objects.requireNonNull(appearances, "appearances")
                .observeAppearance(appearance -> {
                    bridge.applyAppearance(appearance);
                    execute("window.applyJavaAppearance && window.applyJavaAppearance()");
                });
        diagnosticRegistration = Objects.requireNonNull(diagnosticObserver, "diagnosticObserver")
                .observe(diagnostics -> {
                    bridge.applyDiagnostics(MonacoDiagnosticsJson.encode(diagnostics));
                    executeOnJavaFxThread("window.applyJavaDiagnostics && window.applyJavaDiagnostics()");
                });
        WebEngine engine = view.getEngine();
        engine.getLoadWorker().stateProperty().addListener((ignored, previous, state) -> {
            if (state == Worker.State.SUCCEEDED && !closed) {
                JavaFxMonacoInterop.attachBridge(engine, bridge);
            }
        });
        engine.load(MonacoResources.editorPage().toExternalForm());
    }

    private static EditorExtensionHost requireHost(EditorExtensionHost host) {
        return Objects.requireNonNull(host, "extensions");
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

    /** Reveals and selects a zero-based source range. */
    public void reveal(EditorTextRange range) {
        bridge.applySelection(Objects.requireNonNull(range, "range"));
        executeOnJavaFxThread("window.applyJavaSelection && window.applyJavaSelection()");
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            diagnosticRegistration.close();
            appearanceRegistration.close();
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

    private void executeOnJavaFxThread(String script) {
        if (Platform.isFxApplicationThread()) {
            execute(script);
        } else {
            Platform.runLater(() -> execute(script));
        }
    }

    @FunctionalInterface
    private interface DiagnosticObserver {
        EditorRegistration observe(Consumer<List<EditorDiagnosticSnapshot>> observer);
    }

    /** Public bridge methods invoked reflectively by JavaFX WebKit. */
    public static final class Bridge {
        private final EditorTextFileWorkingCopy workingCopy;
        private final EditorLanguageId language;
        private final Runnable stateChanged;
        private final Consumer<String> initializationFailure;
        private final Consumer<IOException> saveFailure;
        private boolean bridgeClosed;
        private String appearance = "{}";
        private String diagnostics = "[]";
        private String selection = "null";

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

        /** Returns the current resolved appearance as JSON consumed by the bundled Monaco page. */
        public String appearance() {
            return appearance;
        }

        /** Returns current Monaco marker data as JSON. */
        public String diagnostics() {
            return diagnostics;
        }

        /** Returns the pending source selection as JSON. */
        public String selection() {
            return selection;
        }

        /** Receives source changes from Monaco. */
        public void contentChanged(String content, Object changes) {
            if (!bridgeClosed) {
                workingCopy.update(content, JavaFxMonacoInterop.textEdits(Objects.requireNonNull(changes, "changes")));
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

        private void applyAppearance(EditorAppearanceSnapshot replacement) {
            appearance = MonacoAppearanceJson.encode(Objects.requireNonNull(replacement, "replacement"));
        }

        private void applyDiagnostics(String replacement) {
            diagnostics = Objects.requireNonNull(replacement, "replacement");
        }

        private void applySelection(EditorTextRange range) {
            selection = "{\"startLineNumber\":" + (range.start().line() + 1)
                    + ",\"startColumn\":" + (range.start().character() + 1)
                    + ",\"endLineNumber\":" + (range.end().line() + 1)
                    + ",\"endColumn\":" + (range.end().character() + 1) + "}";
        }

        private void close() {
            bridgeClosed = true;
        }
    }
}
