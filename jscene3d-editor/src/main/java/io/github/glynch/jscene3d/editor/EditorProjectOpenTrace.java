/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.telemetry.Telemetry;
import io.github.glynch.jscene3d.telemetry.TelemetryOperation;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Owns one project-open trace as it crosses from JavaFX loading to OpenGL presentation. */
public final class EditorProjectOpenTrace {
    private final TelemetryOperation root;

    private Duration projectLoad = Duration.ZERO;
    private Duration previewComposition = Duration.ZERO;
    private Optional<Duration> firstPresentation = Optional.empty();
    private boolean complete;

    /** Starts one trace with the normalized project location as local context. */
    public EditorProjectOpenTrace(Telemetry telemetry, Path projectDirectory) {
        Objects.requireNonNull(telemetry, "telemetry");
        Path normalized = Objects.requireNonNull(projectDirectory, "projectDirectory")
                .toAbsolutePath()
                .normalize();
        root = telemetry.begin("editor.project.open", Map.of("project.path", normalized.toString()));
    }

    /** Measures project loading while exposing its operation for detailed child phases. */
    synchronized <T> T load(Function<TelemetryOperation, T> action) {
        requireIncomplete();
        Objects.requireNonNull(action, "action");
        TelemetryOperation operation = root.begin("editor.project.load", Map.of());
        boolean failed = false;
        try {
            return action.apply(operation);
        } catch (RuntimeException failure) {
            failed = true;
            operation.fail(failure);
            root.fail(failure);
            throw failure;
        } finally {
            operation.close();
            projectLoad = operation.elapsed();
            if (failed) {
                closeRoot();
            }
        }
    }

    /** Measures render-thread preview composition. */
    synchronized <T> T compose(Supplier<T> action) {
        requireIncomplete();
        Objects.requireNonNull(action, "action");
        TelemetryOperation operation = root.begin("editor.preview.compose", Map.of());
        try {
            return action.get();
        } catch (RuntimeException failure) {
            operation.fail(failure);
            root.fail(failure);
            throw failure;
        } finally {
            operation.close();
            previewComposition = operation.elapsed();
        }
    }

    /** Measures the first rendered and presented frame, then completes the trace. */
    synchronized EditorProjectOpenDurations present(Runnable action) {
        requireIncomplete();
        Objects.requireNonNull(action, "action");
        TelemetryOperation operation = root.begin("editor.preview.first-presentation", Map.of());
        try {
            action.run();
        } catch (RuntimeException failure) {
            operation.fail(failure);
            root.fail(failure);
            throw failure;
        } finally {
            operation.close();
            firstPresentation = Optional.of(operation.elapsed());
            closeRoot();
        }
        return durations();
    }

    /** Completes a trace which cannot reach first presentation. */
    public synchronized EditorProjectOpenDurations fail(String message) {
        if (!complete) {
            root.fail(message);
            closeRoot();
        }
        return durations();
    }

    /** Completes a trace after an unexpected failure. */
    public synchronized void fail(Throwable failure) {
        if (!complete) {
            root.fail(failure);
            closeRoot();
        }
    }

    /** Completes a request superseded before it reaches the viewport. */
    synchronized void cancel() {
        fail("project open was superseded");
    }

    /** Returns the durations captured so far, including the fixed total after completion. */
    synchronized EditorProjectOpenDurations durations() {
        return new EditorProjectOpenDurations(root.elapsed(), projectLoad, previewComposition, firstPresentation);
    }

    /** Closes the trace root exactly once. */
    private void closeRoot() {
        root.close();
        complete = true;
    }

    /** Rejects attempts to append phases after trace completion. */
    private void requireIncomplete() {
        if (complete) {
            throw new IllegalStateException("project-open trace is already complete");
        }
    }
}
