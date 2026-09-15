/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildKind;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildRequest;
import io.github.glynch.jscene3d.editor.workbench.build.preference.InMemoryWorkspaceBuildPreferences;
import io.github.glynch.jscene3d.editor.workbench.build.presentation.ProjectBuildFeedbackExtension;
import io.github.glynch.jscene3d.editor.workbench.build.testing.ControllableProjectBuildAdapter;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuCommandSnapshot;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuSnapshot;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies project-scoped build lifecycle through commands and observable menu state. */
final class EditorProjectBuildSessionTest {
    @Test
    void startsInitialBuildAndRoutesCommandsThroughTheOwnedCoordinator() {
        ControllableProjectBuildAdapter adapter = new ControllableProjectBuildAdapter();
        InMemoryWorkspaceBuildPreferences preferences = new InMemoryWorkspaceBuildPreferences();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();
        Path workspace = Path.of("projects/example").toAbsolutePath().normalize();

        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            try (EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host, preferences, feedback, ignored -> Optional.of(adapter), Runnable::run)) {
                host.observeMenus(snapshots::add);

                session.openWorkspace(workspace);

                assertThat(adapter.requests())
                        .containsExactly(new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL));
                assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                                .state()
                                .enabled())
                        .isFalse();
                assertThat(command(snapshots.getLast(), EditorCommands.CANCEL_BUILD)
                                .state()
                                .enabled())
                        .isTrue();

                adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

                assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                                .state()
                                .enabled())
                        .isTrue();
                assertThat(command(snapshots.getLast(), EditorCommands.CANCEL_BUILD)
                                .state()
                                .enabled())
                        .isFalse();
                assertThat(command(snapshots.getLast(), EditorCommands.SHOW_BUILD_OUTPUT)
                                .state()
                                .enabled())
                        .isTrue();

                host.execute(EditorCommands.BUILD_PROJECT);

                assertThat(adapter.requests())
                        .containsExactly(
                                new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL),
                                new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL));

                host.execute(EditorCommands.CANCEL_BUILD);

                assertThat(adapter.activeBuildWasCancelled()).isTrue();
                adapter.completeActive(ProjectBuildOutcome.CANCELLED);
                host.execute(EditorCommands.REBUILD_PROJECT);

                assertThat(adapter.requests())
                        .containsExactly(
                                new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL),
                                new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL),
                                new ProjectBuildRequest(0, ProjectBuildKind.CLEAN));
            }
        }
    }

    @Test
    void opensTheRegisteredBuildOutputViewAfterACompletedBuild() {
        ControllableProjectBuildAdapter adapter = new ControllableProjectBuildAdapter();
        InMemoryWorkspaceBuildPreferences preferences = new InMemoryWorkspaceBuildPreferences();
        EditorProjectContext projects = new EditorProjectContext();
        List<ViewId> requestedViews = new ArrayList<>();
        Path workspace = Path.of("projects/build-output").toAbsolutePath().normalize();

        try (EditorExtensionHost host = host(projects)) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            try (EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host, preferences, feedback, ignored -> Optional.of(adapter), Runnable::run)) {
                host.observeViewRequests(requestedViews::add);
                showProject(projects, workspace);
                session.openWorkspace(workspace);
                adapter.completeActive(ProjectBuildOutcome.SUCCEEDED);

                host.execute(EditorCommands.SHOW_BUILD_OUTPUT);

                assertThat(requestedViews).containsExactly(ProjectBuildFeedbackExtension.OUTPUT_VIEW_ID);
            }
        }
    }

    @Test
    void waitsForACommandWhenAutomaticBuildIsDisabled() {
        ControllableProjectBuildAdapter adapter = new ControllableProjectBuildAdapter();
        InMemoryWorkspaceBuildPreferences preferences = new InMemoryWorkspaceBuildPreferences();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();
        Path workspace = Path.of("projects/manual-build").toAbsolutePath().normalize();
        preferences.saveAutomaticBuild(workspace, false);

        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            try (EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host, preferences, feedback, ignored -> Optional.of(adapter), Runnable::run)) {
                host.observeMenus(snapshots::add);

                session.openWorkspace(workspace);

                assertThat(adapter.requests()).isEmpty();
                assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                                .state()
                                .enabled())
                        .isTrue();

                host.execute(EditorCommands.TOGGLE_AUTOMATIC_BUILD);

                assertThat(adapter.requests())
                        .containsExactly(new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL));
                assertThat(preferences.automaticBuild(workspace)).isTrue();
            }
        }
    }

    @Test
    void cancelsActiveBuildAndDisablesCommandsWhenWorkspaceCloses() {
        ControllableProjectBuildAdapter adapter = new ControllableProjectBuildAdapter();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();

        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            try (EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host,
                    new InMemoryWorkspaceBuildPreferences(),
                    feedback,
                    ignored -> Optional.of(adapter),
                    Runnable::run)) {
                host.observeMenus(snapshots::add);
                session.openWorkspace(Path.of("projects/closing"));

                session.closeWorkspace();

                assertThat(adapter.activeBuildWasCancelled()).isTrue();
                assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                                .state()
                                .enabled())
                        .isFalse();
                assertThat(command(snapshots.getLast(), EditorCommands.CANCEL_BUILD)
                                .state()
                                .enabled())
                        .isFalse();
            }
        }
    }

    @Test
    void keepsBuildCommandsUnavailableForAnUnsupportedWorkspace() {
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();

        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            try (EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host,
                    new InMemoryWorkspaceBuildPreferences(),
                    feedback,
                    ignored -> Optional.empty(),
                    Runnable::run)) {
                host.observeMenus(snapshots::add);

                session.openWorkspace(Path.of("projects/unsupported"));

                assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                                .state()
                                .enabled())
                        .isFalse();
                assertThat(command(snapshots.getLast(), EditorCommands.REBUILD_PROJECT)
                                .state()
                                .enabled())
                        .isFalse();
                assertThat(command(snapshots.getLast(), EditorCommands.TOGGLE_AUTOMATIC_BUILD)
                                .state()
                                .enabled())
                        .isTrue();
            }
        }
    }

    @Test
    void ignoresUiUpdatesQueuedByAProjectWhichHasClosed() {
        ControllableProjectBuildAdapter adapter = new ControllableProjectBuildAdapter();
        List<Runnable> queuedUpdates = new ArrayList<>();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();

        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            try (EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host,
                    new InMemoryWorkspaceBuildPreferences(),
                    feedback,
                    ignored -> Optional.of(adapter),
                    queuedUpdates::add)) {
                host.observeMenus(snapshots::add);
                session.openWorkspace(Path.of("projects/closing-before-ui-update"));
                session.closeWorkspace();

                queuedUpdates.forEach(Runnable::run);

                assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                                .state()
                                .enabled())
                        .isFalse();
                assertThat(command(snapshots.getLast(), EditorCommands.CANCEL_BUILD)
                                .state()
                                .enabled())
                        .isFalse();
            }
        }
    }

    @Test
    void rejectsOpeningAWorkspaceAfterTheSessionCloses() {
        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = feedback(host);
            EditorProjectBuildSession session = new EditorProjectBuildSession(
                    host,
                    new InMemoryWorkspaceBuildPreferences(),
                    feedback,
                    ignored -> Optional.empty(),
                    Runnable::run);
            session.close();
            session.close();
            Path workspace = Path.of("projects/closed");

            assertThatThrownBy(() -> session.openWorkspace(workspace))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("closed");
        }
    }

    private static EditorMenuCommandSnapshot command(List<EditorMenuSnapshot> menus, CommandId command) {
        return menus.stream()
                .flatMap(menu -> menu.commands().stream())
                .filter(item -> item.contribution().id().equals(command))
                .findFirst()
                .orElseThrow();
    }

    private static EditorExtensionHost host() {
        return host(new EditorProjectContext());
    }

    private static EditorExtensionHost host(EditorProjectContext projects) {
        return new EditorExtensionHost(projects, new EditorSelectionContext());
    }

    private static ProjectBuildFeedbackExtension feedback(EditorExtensionHost host) {
        ProjectBuildFeedbackExtension feedback = new ProjectBuildFeedbackExtension();
        host.activate(feedback);
        return feedback;
    }

    private static void showProject(EditorProjectContext projects, Path workspace) {
        EditorSelection selection = new EditorSelection(EditorSelectionKinds.WORLD, "world", Optional.empty());
        EditorHierarchyNode hierarchy = new EditorHierarchyNode(
                EditorHierarchyNode.Kind.WORLD,
                "World",
                Optional.empty(),
                Optional.empty(),
                true,
                selection,
                List.of());
        projects.showProject(new EditorProject("project", "Project", workspace.toUri()), hierarchy, List.of());
    }
}
