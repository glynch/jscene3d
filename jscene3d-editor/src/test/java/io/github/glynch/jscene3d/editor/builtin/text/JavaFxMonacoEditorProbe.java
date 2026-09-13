/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/** JavaFX integration probe that verifies Monaco renders and accepts source changes. */
public final class JavaFxMonacoEditorProbe {
    private static final String INITIAL_SOURCE = "class Initial {}";
    private static final String CHANGED_SOURCE = "class Changed {}";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private JavaFxMonacoEditorProbe() {}

    /** Runs the probe on an initialized JavaFX application thread. */
    public static void verify(Stage stage, Path temporaryDirectory, Consumer<Optional<Throwable>> completion) {
        Path source = null;
        JavaFxMonacoEditor editor = null;
        try {
            source = temporaryDirectory.resolve("MonacoProbe.java");
            Files.writeString(source, INITIAL_SOURCE);
            EditorTextFileWorkingCopy workingCopy = EditorTextFileWorkingCopy.load(source);
            editor = new JavaFxMonacoEditor(workingCopy, EditorLanguages.JAVA, () -> {}, ignored -> {}, ignored -> {});
            WebView webView = (WebView) editor.node();
            stage.setScene(new Scene(webView, 640.0, 400.0));
            pollUntilReady(webView, workingCopy, System.nanoTime() + TIMEOUT.toNanos(), source, editor, completion);
        } catch (IOException | RuntimeException exception) {
            finish(source, editor, completion, Optional.of(exception));
        }
    }

    private static void pollUntilReady(
            WebView webView,
            EditorTextFileWorkingCopy workingCopy,
            long deadline,
            Path source,
            JavaFxMonacoEditor editor,
            Consumer<Optional<Throwable>> completion) {
        if (hasExpectedModel(webView)) {
            webView.getEngine().executeScript("monaco.editor.getModels()[0].setValue('class Changed {}')");
            Optional<Throwable> failure = CHANGED_SOURCE.equals(workingCopy.content())
                    ? Optional.empty()
                    : Optional.of(new AssertionError("Monaco did not publish edited source content"));
            finish(source, editor, completion, failure);
            return;
        }
        if (System.nanoTime() >= deadline) {
            finish(
                    source,
                    editor,
                    completion,
                    Optional.of(new AssertionError("Monaco did not render the opened source file")));
            return;
        }
        PauseTransition retry = new PauseTransition(javafx.util.Duration.millis(100.0));
        retry.setOnFinished(ignored -> pollUntilReady(webView, workingCopy, deadline, source, editor, completion));
        retry.play();
    }

    private static boolean hasExpectedModel(WebView webView) {
        try {
            return Boolean.TRUE.equals(webView.getEngine()
                    .executeScript("typeof monaco !== 'undefined'"
                            + " && monaco.editor.getModels().length === 1"
                            + " && monaco.editor.getModels()[0].getValue() === 'class Initial {}'"));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static void finish(
            Path source,
            JavaFxMonacoEditor editor,
            Consumer<Optional<Throwable>> completion,
            Optional<Throwable> failure) {
        if (editor != null) {
            editor.close();
        }
        if (source != null) {
            try {
                Files.deleteIfExists(source);
            } catch (IOException exception) {
                failure.ifPresentOrElse(
                        existing -> existing.addSuppressed(exception), () -> completion.accept(Optional.of(exception)));
                if (failure.isEmpty()) {
                    return;
                }
            }
        }
        completion.accept(failure);
    }
}
