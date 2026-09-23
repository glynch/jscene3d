/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.authoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectCloseResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenParams;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectOpenResult;
import io.github.glynch.jscene3d.editor.authoring.protocol.ProjectSummary;
import io.github.glynch.jscene3d.editor.authoring.testing.AuthoringTestProject;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exercises retained-session ownership through the real safe authoring loader. */
final class AuthoringProjectServiceTest {
    @TempDir
    private Path temporaryDirectory;

    private final AuthoringProjectService service = new AuthoringProjectService(
            new EditorProjectLoader("0.1.0-SNAPSHOT", AuthoringProjectServiceTest.class.getClassLoader()));

    /** Releases retained sessions even after assertion failures. */
    @AfterEach
    void closeService() {
        service.close();
    }

    /** Opens a current descriptor, retains its session, and returns the narrow project summary. */
    @Test
    void opensJ3dProjectAndReturnsSummary() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);

        ProjectOpenResult result = service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        ProjectSummary summary = Objects.requireNonNull(result.project());

        assertThat(result.opened()).isTrue();
        assertThat(result.projectGeneration()).isEqualTo(1L);
        assertThat(result.diagnostics()).isEmpty();
        assertThat(result.failureCode()).isNull();
        assertThat(summary)
                .returns("example.authoring-test", project -> project.id())
                .returns("Small Authoring Project", project -> project.name())
                .returns("1.2.3", project -> project.version())
                .returns(temporaryDirectory.toRealPath().toString(), project -> project.root())
                .returns(
                        temporaryDirectory
                                .toRealPath()
                                .resolve(AuthoringTestProject.DESCRIPTOR)
                                .toString(),
                        project -> project.descriptor())
                .satisfies(project -> {
                    assertThat(project.startupWorld().id()).isEqualTo("e890c4c3-fb32-49d8-88b8-4e04e7a29656");
                    assertThat(project.startupWorld().name()).isEqualTo("Opening World");
                    assertThat(project.assetCounts().authored()).isEqualTo(1);
                    assertThat(project.assetCounts().projected()).isEqualTo(1);
                });
        assertThat(service.activeSession()).isPresent();
    }

    /** Accepts the selected descriptor path without encoding its filename in the protocol. */
    @Test
    void opensSelectedDescriptorPath() throws IOException {
        String descriptorName = "custom-game-name.j3d";
        AuthoringTestProject.write(temporaryDirectory, descriptorName);

        ProjectOpenResult result = service.openProject(
                new ProjectOpenParams(temporaryDirectory.resolve(descriptorName).toString()));

        assertThat(result.opened()).isTrue();
        assertThat(Objects.requireNonNull(result.project()).descriptor()).endsWith(descriptorName);
    }

    /** Explicit close releases the session without terminating the process-scoped service. */
    @Test
    void closesActiveProject() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        ProjectCloseResult result = service.closeProject();

        assertThat(result.closed()).isTrue();
        assertThat(result.invalidatedProjectGeneration()).isEqualTo(1L);
        assertThat(retained.isClosed()).isTrue();
        assertThat(service.activeSession()).isEmpty();
        assertThat(service.isClosed()).isFalse();
    }

    /** Closing with no project is an idempotent no-op. */
    @Test
    void closesWhenNoProjectIsOpen() {
        ProjectCloseResult result = service.closeProject();

        assertThat(result.closed()).isFalse();
        assertThat(result.invalidatedProjectGeneration()).isNull();
    }

    /** A close permits another project with a strictly newer session generation. */
    @Test
    void opensClosesAndReopens() throws IOException {
        Path first = temporaryDirectory.resolve("first");
        Path second = temporaryDirectory.resolve("second");
        AuthoringTestProject.write(first, "first.j3d", "First Project");
        AuthoringTestProject.write(second, "second.j3d", "Second Project");

        ProjectOpenResult firstResult = service.openProject(new ProjectOpenParams(first.toString()));
        service.closeProject();
        ProjectOpenResult secondResult = service.openProject(new ProjectOpenParams(second.toString()));

        assertThat(firstResult.projectGeneration()).isEqualTo(1L);
        assertThat(secondResult.projectGeneration()).isEqualTo(2L);
        assertThat(Objects.requireNonNull(secondResult.project()).name()).isEqualTo("Second Project");
    }

    /** A second open is rejected rather than silently replacing the retained session. */
    @Test
    void rejectsSecondOpenWhileProjectIsActive() throws IOException {
        Path first = temporaryDirectory.resolve("first");
        Path second = temporaryDirectory.resolve("second");
        AuthoringTestProject.write(first, "first.j3d");
        AuthoringTestProject.write(second, "second.j3d");
        service.openProject(new ProjectOpenParams(first.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        ProjectOpenResult rejected = service.openProject(new ProjectOpenParams(second.toString()));

        assertThat(rejected.opened()).isFalse();
        assertThat(rejected.failureCode()).isEqualTo(AuthoringProjectService.PROJECT_ALREADY_OPEN);
        assertThat(service.activeSession()).containsSame(retained);
        assertThat(retained.isClosed()).isFalse();
    }

    /** An invalid project remains a domain result with structured diagnostics. */
    @Test
    void reportsInvalidProject() {
        ProjectOpenResult result = service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        assertThat(result.opened()).isFalse();
        assertThat(result.project()).isNull();
        assertThat(result.diagnostics())
                .singleElement()
                .returns("error", diagnostic -> diagnostic.severity())
                .returns("project.descriptor.missing", diagnostic -> diagnostic.code())
                .satisfies(diagnostic -> assertThat(diagnostic.source()).startsWith("file:"));
        assertThat(service.activeSession()).isEmpty();
    }

    /** The legacy descriptor opens with its real path and deprecation diagnostic intact. */
    @Test
    void opensLegacyProjectWithDeprecationDiagnostic() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, "jscene3d.json");

        ProjectOpenResult result = service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));

        assertThat(result.opened()).isTrue();
        assertThat(Objects.requireNonNull(result.project()).descriptor()).endsWith("jscene3d.json");
        assertThat(result.diagnostics())
                .singleElement()
                .returns("warning", diagnostic -> diagnostic.severity())
                .returns("project.descriptor.legacy", diagnostic -> diagnostic.code())
                .satisfies(diagnostic -> assertThat(diagnostic.source()).endsWith("jscene3d.json"));
    }

    /** Process-scoped close releases a retained session and rejects later operations. */
    @Test
    void cleansUpRetainedSessionOnServiceClose() throws IOException {
        AuthoringTestProject.write(temporaryDirectory, AuthoringTestProject.DESCRIPTOR);
        service.openProject(new ProjectOpenParams(temporaryDirectory.toString()));
        EditorProjectSession retained = service.activeSession().orElseThrow();

        service.close();

        assertThat(service.isClosed()).isTrue();
        assertThat(retained.isClosed()).isTrue();
        assertThatThrownBy(service::activeSession)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }
}
