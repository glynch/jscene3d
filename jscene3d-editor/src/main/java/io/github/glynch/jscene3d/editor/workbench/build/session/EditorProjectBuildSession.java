/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.session;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.build.command.EditorProjectBuildCommandSet;
import io.github.glynch.jscene3d.editor.workbench.build.command.ProjectBuildCommandAvailability;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildCompletion;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildCoordinator;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildKind;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildPhase;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildSnapshot;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildAdapter;
import io.github.glynch.jscene3d.editor.workbench.build.maven.MavenProjectBuildAdapter;
import io.github.glynch.jscene3d.editor.workbench.build.preference.WorkspaceBuildPreferences;
import io.github.glynch.jscene3d.editor.workbench.build.presentation.ProjectBuildFeedbackExtension;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.application.Platform;
import org.jspecify.annotations.Nullable;

/** Owns build commands and transient build activity for the active project. */
public final class EditorProjectBuildSession implements AutoCloseable {
    private final WorkspaceBuildPreferences preferences;
    private final Function<Path, Optional<ProjectBuildAdapter>> adapterSelector;
    private final Consumer<Runnable> uiDispatcher;
    private final ProjectBuildFeedbackExtension feedback;
    private final EditorProjectBuildCommandSet commands;

    private EditorRegistration observation = () -> {};
    private EditorRegistration completionObservation = () -> {};
    private volatile @Nullable ProjectBuildCoordinator coordinator;
    private boolean closed;

    /**
     * Creates the project build session used by the JavaFX workbench.
     *
     * @param extensions workbench command and menu host
     * @param preferences per-workspace build preferences
     * @param feedback build status and output presenter
     * @param buildExecutor caller-owned executor used to observe Maven processes
     */
    public EditorProjectBuildSession(
            EditorExtensionHost extensions,
            WorkspaceBuildPreferences preferences,
            ProjectBuildFeedbackExtension feedback,
            Executor buildExecutor) {
        this(
                extensions,
                preferences,
                feedback,
                projectRoot -> selectAdapter(projectRoot, buildExecutor),
                EditorProjectBuildSession::dispatch);
    }

    EditorProjectBuildSession(
            EditorExtensionHost extensions,
            WorkspaceBuildPreferences preferences,
            ProjectBuildFeedbackExtension feedback,
            Function<Path, Optional<ProjectBuildAdapter>> adapterSelector,
            Consumer<Runnable> uiDispatcher) {
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.adapterSelector = Objects.requireNonNull(adapterSelector, "adapterSelector");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        commands = new EditorProjectBuildCommandSet(
                Objects.requireNonNull(extensions, "extensions"),
                preferences,
                new EditorProjectBuildCommandSet.Actions(
                        () -> request(ProjectBuildKind.INCREMENTAL),
                        () -> request(ProjectBuildKind.CLEAN),
                        this::cancel,
                        () -> extensions.showView(ProjectBuildFeedbackExtension.OUTPUT_VIEW_ID),
                        this::automaticBuildChanged));
    }

    /**
     * Opens build support for a project and starts its initial automatic build when supported.
     *
     * @param projectRoot project workspace root
     */
    public void openWorkspace(Path projectRoot) {
        ensureOpen();
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        closeCoordinator();
        commands.openWorkspace(root);
        feedback.openWorkspace(root);
        Optional<ProjectBuildAdapter> selected = adapterSelector.apply(root);
        if (selected.isEmpty()) {
            commands.update(ProjectBuildCommandAvailability.UNAVAILABLE);
            feedback.closeWorkspace();
            return;
        }

        ProjectBuildCoordinator opened =
                new ProjectBuildCoordinator(selected.orElseThrow(), preferences.automaticBuild(root));
        coordinator = opened;
        observation = opened.observe(snapshot -> publish(opened, snapshot));
        completionObservation = opened.observeCompletions(completion -> publish(opened, completion));
        if (preferences.automaticBuild(root)) {
            opened.request(ProjectBuildKind.INCREMENTAL);
        }
    }

    /** Closes the active project's build activity and disables its commands. */
    public void closeWorkspace() {
        if (closed) {
            return;
        }
        closeCoordinator();
        commands.closeWorkspace();
        feedback.closeWorkspace();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closeCoordinator();
        commands.closeWorkspace();
        feedback.closeWorkspace();
        commands.close();
        closed = true;
    }

    private void request(ProjectBuildKind kind) {
        ProjectBuildCoordinator current = coordinator;
        if (current != null) {
            current.request(kind);
        }
    }

    private void cancel() {
        ProjectBuildCoordinator current = coordinator;
        if (current != null) {
            current.cancel();
        }
    }

    private void automaticBuildChanged(boolean enabled) {
        ProjectBuildCoordinator current = coordinator;
        if (current != null) {
            current.automaticBuild(enabled);
        }
    }

    private void publish(ProjectBuildCoordinator source, ProjectBuildSnapshot snapshot) {
        uiDispatcher.accept(() -> {
            if (coordinator == source) {
                feedback.show(snapshot);
                commands.update(availability(snapshot, feedback.outputAvailable()));
            }
        });
    }

    private void publish(ProjectBuildCoordinator source, ProjectBuildCompletion completion) {
        uiDispatcher.accept(() -> {
            if (coordinator == source) {
                ProjectBuildSnapshot snapshot = source.snapshot();
                feedback.show(completion, snapshot.savedRevision());
                commands.update(availability(snapshot, true));
            }
        });
    }

    private void closeCoordinator() {
        ProjectBuildCoordinator current = coordinator;
        coordinator = null;
        observation.close();
        observation = () -> {};
        completionObservation.close();
        completionObservation = () -> {};
        if (current != null) {
            current.close();
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("project build session is closed");
        }
    }

    private static ProjectBuildCommandAvailability availability(
            ProjectBuildSnapshot snapshot, boolean outputAvailable) {
        ProjectBuildPhase phase = snapshot.phase();
        boolean building = phase == ProjectBuildPhase.QUEUED || phase == ProjectBuildPhase.BUILDING;
        return new ProjectBuildCommandAvailability(true, building, outputAvailable);
    }

    private static Optional<ProjectBuildAdapter> selectAdapter(Path projectRoot, Executor executor) {
        return MavenProjectBuildAdapter.supports(projectRoot)
                ? Optional.of(new MavenProjectBuildAdapter(projectRoot, Objects.requireNonNull(executor, "executor")))
                : Optional.empty();
    }

    private static void dispatch(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
