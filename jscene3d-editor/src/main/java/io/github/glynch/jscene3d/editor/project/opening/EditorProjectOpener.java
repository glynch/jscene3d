/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.opening;

import static io.github.glynch.jscene3d.editor.command.EditorCommands.OPEN_DIAGNOSTICS;
import static io.github.glynch.jscene3d.editor.window.EditorMessageSeverity.ERROR;

import io.github.glynch.jscene3d.editor.EditorDiagnosticCode;
import io.github.glynch.jscene3d.editor.EditorLoadingPhase;
import io.github.glynch.jscene3d.editor.EditorPreviewResult;
import io.github.glynch.jscene3d.editor.EditorProjectLoadResult;
import io.github.glynch.jscene3d.editor.EditorProjectLoadTask;
import io.github.glynch.jscene3d.editor.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.EditorProjectOpenDurations;
import io.github.glynch.jscene3d.editor.EditorProjectOpenTrace;
import io.github.glynch.jscene3d.editor.EditorProjectSession;
import io.github.glynch.jscene3d.editor.EditorSplashScreen;
import io.github.glynch.jscene3d.editor.EditorWorkspace;
import io.github.glynch.jscene3d.editor.ViewportController;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.telemetry.Telemetry;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Opens projects into the editor and owns their complete asynchronous presentation workflow. */
public final class EditorProjectOpener implements AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(EditorProjectOpener.class.getName());

    private final Telemetry telemetry;
    private final EditorProjectLoader loader;
    private final ExecutorService executor;
    private final EditorProjectPublication publication;
    private final EditorWorkspace workspace;
    private final ViewportController viewport;
    private final EditorSplashScreen splash;
    private final Consumer<String> windowTitle;

    /**
     * Creates the project-opening workflow around the already constructed editor runtime.
     *
     * @param telemetry project-opening telemetry sink
     * @param loader background project loader
     * @param publication extension-facing project and diagnostic publication
     * @param workspace visible editor workbench
     * @param viewport editor preview controller
     * @param splash project-loading presentation
     * @param windowTitle window-title sink
     */
    public EditorProjectOpener(
            Telemetry telemetry,
            EditorProjectLoader loader,
            EditorProjectPublication publication,
            EditorWorkspace workspace,
            ViewportController viewport,
            EditorSplashScreen splash,
            Consumer<String> windowTitle) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.loader = Objects.requireNonNull(loader, "loader");
        executor = Executors.newSingleThreadExecutor();
        this.publication = Objects.requireNonNull(publication, "publication");
        this.workspace = Objects.requireNonNull(workspace, "workspace");
        this.viewport = Objects.requireNonNull(viewport, "viewport");
        this.splash = Objects.requireNonNull(splash, "splash");
        this.windowTitle = Objects.requireNonNull(windowTitle, "windowTitle");
    }

    /**
     * Opens one project without blocking JavaFX rendering or preview presentation.
     *
     * @param directory project directory to open
     */
    public void openProject(Path directory) {
        Path normalized =
                Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        EditorProjectOpenTrace trace = new EditorProjectOpenTrace(telemetry, normalized);
        publication.clearProject();
        workspace.beginOpening(normalized);
        splash.showProject(normalized);
        EditorProjectLoadTask task = new EditorProjectLoadTask(loader, trace, normalized, splash);
        task.setOnSucceeded(ignored -> applyLoadedProject(trace, task.getValue()));
        task.setOnFailed(ignored -> handleProjectLoadFailure(trace, normalized, task.getException()));
        executor.execute(task);
    }

    /** Stops accepting project work and interrupts a load that is still in progress. */
    @Override
    public void close() {
        executor.shutdownNow();
    }

    /** Applies background-loaded editor state and queues its preview on the OpenGL thread. */
    private void applyLoadedProject(EditorProjectOpenTrace trace, EditorProjectLoadResult result) {
        publication.showDiagnostics(result.diagnostics());
        if (result.session().isEmpty()) {
            trace.fail("project loading did not create an editor session");
            clearFailedProject("Unable to open project. See Diagnostics.");
            return;
        }
        EditorProjectSession session = result.session().orElseThrow();
        workspace.showProject(session);
        publication.showProject(session);
        windowTitle.accept(session.project().identity().name() + " — JScene3D Editor");
        splash.projectIdentified(session.project().identity().name());
        splash.phaseStarted(EditorLoadingPhase.PREPARING_PREVIEW);
        viewport.showProject(
                session, trace, completion -> applyPreviewResult(result.diagnostics(), session, completion));
        workspace.setProjectStatus("Preparing " + session.project().identity().name() + "…");
    }

    /** Restores an empty, usable editor after an unexpected background-loading failure. */
    private void handleProjectLoadFailure(EditorProjectOpenTrace trace, Path projectRoot, Throwable failure) {
        trace.fail(failure);
        ProjectDiagnostic diagnostic = new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                EditorDiagnosticCode.PROJECT_LOAD_FAILED,
                projectRoot.toUri(),
                "",
                Map.of("technicalDetail", Objects.requireNonNullElse(failure.getMessage(), failure.toString())));
        publication.showDiagnostics(List.of(diagnostic));
        LOGGER.log(System.Logger.Level.ERROR, "Editor project loading failed", failure);
        clearFailedProject("Unable to open project. See Diagnostics.");
    }

    /** Applies preview diagnostics, reports completion, and dismisses the project-loading splash. */
    private void applyPreviewResult(
            List<ProjectDiagnostic> loadDiagnostics, EditorProjectSession session, EditorPreviewResult result) {
        List<ProjectDiagnostic> combined = new ArrayList<>(loadDiagnostics);
        combined.addAll(result.diagnostics());
        publication.showDiagnostics(combined);
        boolean failed = result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.severity() == ProjectDiagnostic.Severity.ERROR);
        if (failed) {
            workspace.showMessage(new EditorMessage(
                    ERROR, "Unable to prepare the project preview. See Diagnostics.", OPEN_DIAGNOSTICS));
            splash.finish();
            return;
        }
        EditorProjectOpenDurations durations = result.durations();
        String firstFrame =
                durations.firstPresentation().map(EditorProjectOpener::format).orElse("not presented");
        LOGGER.log(
                System.Logger.Level.INFO,
                "Opened " + session.project().identity().name() + " in "
                        + format(durations.total())
                        + " (project " + format(durations.projectLoad())
                        + ", preview " + format(durations.previewComposition())
                        + ", first frame " + firstFrame + ")");
        workspace.setProjectStatus(session.project().identity().name() + " · ready");
        splash.finish();
    }

    /** Clears project-owned state while preserving diagnostics and a visible failure message. */
    private void clearFailedProject(String message) {
        publication.clearProject();
        workspace.clearProject();
        viewport.clearProject();
        workspace.showMessage(new EditorMessage(ERROR, message, OPEN_DIAGNOSTICS));
        splash.finish();
    }

    /** Formats a measured duration in milliseconds with useful sub-millisecond precision. */
    private static String format(Duration duration) {
        double milliseconds = duration.toNanos() / 1_000_000.0;
        return String.format(Locale.ROOT, "%.1f ms", milliseconds);
    }
}
