/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildKind;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildOutcome;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildRequest;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildExecution;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildResult;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies Maven execution through the generic project-build adapter seam. */
final class MavenProjectBuildAdapterTest {
    @Test
    void detectsMavenProjectsByTheirDescriptor(@TempDir Path temporaryDirectory) throws Exception {
        Path mavenProject = temporaryDirectory.resolve("maven-project");
        MavenBuildTestProject.create(mavenProject);
        Path plainDirectory = temporaryDirectory.resolve("plain-directory");
        Files.createDirectories(plainDirectory);

        assertThat(MavenProjectBuildAdapter.supports(mavenProject)).isTrue();
        assertThat(MavenProjectBuildAdapter.supports(plainDirectory)).isFalse();
    }

    @Test
    void executesIncrementalBuildWithProjectWrapper(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(7, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.SUCCEEDED);
        assertThat(project.invocations()).containsExactly("compile");
    }

    @Test
    void preservesCommandOutputAndDuration(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(3, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.SUCCEEDED);
        assertThat(result.command()).endsWith("compile");
        assertThat(result.command()).contains(project.wrapper().toString());
        assertThat(result.standardOutput()).contains("fixture build completed");
        assertThat(result.standardError()).contains("fixture build details");
        assertThat(result.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    @Test
    void executesCleanBuildIntentAsCleanCompile(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(4, ProjectBuildKind.CLEAN))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.SUCCEEDED);
        assertThat(project.invocations()).containsExactly("clean compile");
    }

    @Test
    void reportsWrapperFailureAndPreservesItsOutput(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        project.failBuild();
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(5, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.FAILED);
        assertThat(result.standardOutput()).contains("fixture compilation failed");
        assertThat(result.standardError()).contains("[ERROR] Example.java:[4,9] cannot find symbol");
    }

    @Test
    void cancellationTerminatesTheCompleteWrapperProcessTree(@TempDir Path temporaryDirectory) throws Exception {
        assumeFalse(OperatingSystem.current() == OperatingSystem.WINDOWS);
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        project.blockBuild();
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root());
        ProjectBuildExecution execution = adapter.start(new ProjectBuildRequest(6, ProjectBuildKind.INCREMENTAL));
        ProcessHandle child = project.awaitBlockingChild();

        try {
            execution.cancel();
            ProjectBuildResult result =
                    execution.completion().toCompletableFuture().get(5, SECONDS);

            assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.CANCELLED);
            assertThat(child.isAlive()).isFalse();
        } finally {
            child.destroyForcibly();
        }
    }
}
