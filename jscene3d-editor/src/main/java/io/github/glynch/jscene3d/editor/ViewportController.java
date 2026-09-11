/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import com.huskerdev.openglfx.canvas.events.GLRenderEvent;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.render.RenderSurfaceSize;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.jspecify.annotations.Nullable;

/** Coordinates OpenGLFX callbacks, renderer lifecycle, and visible viewport status. */
final class ViewportController {
    private static final System.Logger LOGGER = System.getLogger(ViewportController.class.getName());

    private final GLCanvas canvas;
    private final Label status;
    private final Runnable firstFramePresented;
    private final Runnable disposalComplete;
    private final OpenGlFxRenderSurface surface = new OpenGlFxRenderSurface();
    private final AtomicReference<@Nullable PreviewRequest> pendingPreview = new AtomicReference<>();

    private @Nullable EditorPreview preview;
    private boolean focused;
    private boolean firstFrameReported;
    private boolean disposed;

    /** Stores the JavaFX controls and state used by rendering callbacks. */
    ViewportController(GLCanvas canvas, Label status, Runnable firstFramePresented, Runnable disposalComplete) {
        this.canvas = canvas;
        this.status = status;
        this.firstFramePresented = Objects.requireNonNull(firstFramePresented, "firstFramePresented");
        this.disposalComplete = Objects.requireNonNull(disposalComplete, "disposalComplete");
    }

    /** Renders one OpenGLFX frame through the actual JScene3D renderer. */
    void render(GLRenderEvent event) {
        @Nullable PendingPresentation pendingPresentation = null;
        try {
            surface.beginFrame(canvas, event);
            RenderSurfaceSize size = surface.size();
            if (!size.isDrawable()) {
                return;
            }
            EditorPreview currentPreview = preview;
            if (currentPreview == null) {
                currentPreview = new EditorPreview(surface);
                preview = currentPreview;
            }
            pendingPresentation = applyPendingPreview(currentPreview);
            if (pendingPresentation == null) {
                currentPreview.render(size);
                surface.present();
            } else {
                presentFirstFrame(currentPreview, size, pendingPresentation);
            }
            reportFirstFramePresented();
            if (currentPreview.frameCount() % 30L == 0L) {
                updateStatus(event, size);
            }
        } catch (RuntimeException exception) {
            if (pendingPresentation != null) {
                pendingPresentation.request().trace().orElseThrow().fail(exception);
            }
            canvas.setFps(0.0);
            LOGGER.log(System.Logger.Level.ERROR, "Editor viewport rendering failed", exception);
            updateStatus("Preview: failed");
        }
    }

    /** Releases renderer resources from OpenGLFX's context-owning disposal callback. */
    void dispose() {
        if (disposed) {
            return;
        }
        try {
            EditorPreview currentPreview = preview;
            if (currentPreview != null) {
                currentPreview.close();
            }
            LOGGER.log(System.Logger.Level.INFO, "Disposed editor viewport and JScene3D renderer");
        } catch (RuntimeException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Editor viewport disposal failed", exception);
        } finally {
            disposed = true;
            Platform.runLater(disposalComplete);
        }
    }

    /** Requests composition of one editor-safe project preview on the OpenGL rendering thread. */
    void showProject(
            EditorProjectSession session, EditorProjectOpenTrace trace, Consumer<EditorPreviewResult> completion) {
        replacePendingPreview(new PreviewRequest(
                Optional.of(Objects.requireNonNull(session, "session")),
                Optional.of(Objects.requireNonNull(trace, "trace")),
                Objects.requireNonNull(completion, "completion")));
    }

    /** Requests removal of the current project preview after an unsuccessful project open. */
    void clearProject() {
        replacePendingPreview(new PreviewRequest(Optional.empty(), Optional.empty(), ignored -> {}));
    }

    /** Records JavaFX focus changes for the next visible status update. */
    void setFocused(boolean focused) {
        this.focused = focused;
    }

    /** Applies the most recent pending project change and reports composition diagnostics on JavaFX. */
    private @Nullable PendingPresentation applyPendingPreview(EditorPreview currentPreview) {
        @Nullable PreviewRequest request = pendingPreview.getAndSet(null);
        if (request == null) {
            return null;
        }
        if (request.session().isPresent()) {
            EditorProjectOpenTrace trace = request.trace().orElseThrow();
            try {
                List<ProjectDiagnostic> previewDiagnostics = trace.compose(
                        () -> currentPreview.show(request.session().orElseThrow()));
                boolean failed = previewDiagnostics.stream()
                        .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
                if (failed) {
                    EditorProjectOpenDurations durations = trace.fail("preview composition produced errors");
                    complete(request, previewDiagnostics, durations);
                    return null;
                }
                return new PendingPresentation(request, previewDiagnostics);
            } catch (RuntimeException exception) {
                trace.fail(exception);
                throw exception;
            }
        }
        currentPreview.clearProject();
        return null;
    }

    /** Presents a newly composed preview before reporting the project open as complete. */
    private void presentFirstFrame(
            EditorPreview currentPreview, RenderSurfaceSize size, PendingPresentation pendingPresentation) {
        PreviewRequest request = pendingPresentation.request();
        EditorProjectOpenTrace trace = request.trace().orElseThrow();
        EditorProjectOpenDurations durations = trace.present(() -> {
            currentPreview.render(size);
            surface.present();
        });
        complete(request, pendingPresentation.diagnostics(), durations);
    }

    /** Delivers one immutable preview completion on the JavaFX Application Thread. */
    private static void complete(
            PreviewRequest request, List<ProjectDiagnostic> previewDiagnostics, EditorProjectOpenDurations durations) {
        EditorPreviewResult result = new EditorPreviewResult(previewDiagnostics, durations);
        Platform.runLater(() -> request.completion().accept(result));
    }

    /** Reports the first successfully presented viewport frame exactly once on JavaFX. */
    private void reportFirstFramePresented() {
        if (firstFrameReported) {
            return;
        }
        firstFrameReported = true;
        updateStatus("Preview: inert");
        Platform.runLater(firstFramePresented);
    }

    /** Replaces a queued preview request and closes any trace which can no longer complete. */
    private void replacePendingPreview(PreviewRequest request) {
        @Nullable PreviewRequest replaced = pendingPreview.getAndSet(request);
        if (replaced != null) {
            replaced.trace().ifPresent(EditorProjectOpenTrace::cancel);
        }
    }

    /** Formats concise preview state for the persistent status bar. */
    private void updateStatus(GLRenderEvent event, RenderSurfaceSize size) {
        String text = "Preview: inert · "
                + size.framebufferWidth()
                + "×"
                + size.framebufferHeight()
                + " · "
                + event.fps
                + " FPS"
                + (focused ? " · focused" : "");
        updateStatus(text);
    }

    /** Applies a status update on the JavaFX Application Thread. */
    private void updateStatus(String text) {
        if (Platform.isFxApplicationThread()) {
            status.setText(text);
        } else {
            Platform.runLater(() -> status.setText(text));
        }
    }

    /** One immutable cross-thread request to replace or clear the viewport project. */
    private record PreviewRequest(
            Optional<EditorProjectSession> session,
            Optional<EditorProjectOpenTrace> trace,
            Consumer<EditorPreviewResult> completion) {}

    /** Composed preview waiting for its first render and presentation in the current frame. */
    private record PendingPresentation(PreviewRequest request, List<ProjectDiagnostic> diagnostics) {}
}
