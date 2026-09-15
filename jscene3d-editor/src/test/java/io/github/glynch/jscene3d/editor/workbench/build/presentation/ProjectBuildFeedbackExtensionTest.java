/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildCompletion;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildKind;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildPhase;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildRequest;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildSnapshot;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildDiagnostic;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies project-build presentation independently of Maven execution. */
final class ProjectBuildFeedbackExtensionTest {
    @Test
    void presentsBuildLifecycleAndCompleteTerminalOutput(@TempDir Path projectRoot) {
        List<List<EditorStatusItemSnapshot>> statuses = new ArrayList<>();
        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = activate(host);
            host.observeStatusItems(statuses::add);
            feedback.openWorkspace(projectRoot);

            feedback.show(snapshot(ProjectBuildPhase.BUILDING));

            assertThat(statuses.getLast()).singleElement().satisfies(item -> {
                assertThat(item.state().text()).isEqualTo("Building");
                assertThat(item.state().visible()).isTrue();
            });

            feedback.show(
                    completion(ProjectBuildOutcome.SUCCEEDED, "compiled project\n", "compiler detail\n", List.of()), 0);
            feedback.show(snapshot(ProjectBuildPhase.CURRENT));

            assertThat(feedback.output().content())
                    .contains("Build revision 0 (incremental)")
                    .contains("Command: ./mvnw process-classes")
                    .contains("Outcome: succeeded")
                    .contains("Standard output:\ncompiled project")
                    .contains("Standard error:\ncompiler detail");
            assertThat(statuses.getLast()).singleElement().satisfies(item -> {
                assertThat(item.state().text()).isEqualTo("Build succeeded");
                assertThat(item.state().command()).contains(EditorCommands.SHOW_BUILD_OUTPUT);
            });
        }
    }

    @Test
    void publishesStructuredFailureAndClearsItAfterSuccess(@TempDir Path projectRoot) {
        URI source = projectRoot.resolve("src/main/java/example/Example.java").toUri();
        EditorTextPosition position = new EditorTextPosition(3, 8);
        EditorDiagnostic diagnostic = new EditorDiagnostic(
                EditorDiagnosticSeverity.ERROR,
                "maven.compiler",
                "Maven",
                "cannot find symbol",
                "4:9",
                Optional.of(new EditorTextRange(position, new EditorTextPosition(3, 9))),
                Map.of());
        List<List<EditorDiagnosticSnapshot>> publications = new ArrayList<>();
        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = activate(host);
            host.observeDiagnostics(publications::add);
            feedback.openWorkspace(projectRoot);

            feedback.show(
                    completion(
                            ProjectBuildOutcome.FAILED,
                            "",
                            "compilation failure",
                            List.of(new ProjectBuildDiagnostic(source, diagnostic))),
                    0);

            assertThat(publications.getLast()).singleElement().satisfies(item -> {
                assertThat(item.source()).isEqualTo(source);
                assertThat(item.diagnostic().message()).isEqualTo("cannot find symbol");
                assertThat(item.diagnostic().range())
                        .contains(new EditorTextRange(position, new EditorTextPosition(3, 9)));
            });

            feedback.show(completion(ProjectBuildOutcome.SUCCEEDED, "done", "", List.of()), 0);

            assertThat(publications.getLast()).isEmpty();
        }
    }

    @Test
    void publishesProjectLevelDiagnosticForExceptionalFailure(@TempDir Path projectRoot) {
        List<List<EditorDiagnosticSnapshot>> publications = new ArrayList<>();
        try (EditorExtensionHost host = host()) {
            ProjectBuildFeedbackExtension feedback = activate(host);
            host.observeDiagnostics(publications::add);
            feedback.openWorkspace(projectRoot);

            feedback.show(
                    ProjectBuildCompletion.failed(
                            new ProjectBuildRequest(0, ProjectBuildKind.CLEAN),
                            new IllegalStateException("Maven wrapper not found")),
                    0);

            assertThat(publications.getLast()).singleElement().satisfies(item -> {
                assertThat(item.source())
                        .isEqualTo(projectRoot.resolve("pom.xml").toUri());
                assertThat(item.diagnostic().code()).isEqualTo("project.build.failed");
                assertThat(item.diagnostic().message()).isEqualTo("Maven wrapper not found");
            });
        }
    }

    private static ProjectBuildCompletion completion(
            ProjectBuildOutcome outcome,
            String standardOutput,
            String standardError,
            List<ProjectBuildDiagnostic> diagnostics) {
        ProjectBuildRequest request = new ProjectBuildRequest(0, ProjectBuildKind.INCREMENTAL);
        ProjectBuildResult result = new ProjectBuildResult(
                outcome,
                List.of("./mvnw", "process-classes"),
                standardOutput,
                standardError,
                Duration.ofMillis(125),
                diagnostics);
        return ProjectBuildCompletion.completed(request, result);
    }

    private static ProjectBuildSnapshot snapshot(ProjectBuildPhase phase) {
        OptionalLong successful = phase == ProjectBuildPhase.CURRENT ? OptionalLong.of(0) : OptionalLong.empty();
        return new ProjectBuildSnapshot(phase, 0, successful, true, false);
    }

    private static ProjectBuildFeedbackExtension activate(EditorExtensionHost host) {
        ProjectBuildFeedbackExtension feedback = new ProjectBuildFeedbackExtension();
        host.activate(feedback);
        return feedback;
    }

    private static EditorExtensionHost host() {
        return new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
    }
}
