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
import java.util.concurrent.ForkJoinPool;
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
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(7, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.SUCCEEDED);
        assertThat(project.invocations()).containsExactly("process-classes");
    }

    @Test
    void preservesCommandOutputAndDuration(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(3, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.SUCCEEDED);
        assertThat(result.command()).endsWith("process-classes");
        assertThat(result.command()).contains(project.wrapper().toString());
        assertThat(result.standardOutput()).contains("fixture build completed");
        assertThat(result.standardError()).contains("fixture build details");
        assertThat(result.duration()).isGreaterThanOrEqualTo(Duration.ZERO);
    }

    @Test
    void executesCleanBuildIntentThroughGeneratedProjectContent(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(4, ProjectBuildKind.CLEAN))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.SUCCEEDED);
        assertThat(project.invocations()).containsExactly("clean process-classes");
        assertThat(project.compiledOutput()).hasContent("clean process-classes\n");
    }

    @Test
    void reportsWrapperFailureAndPreservesItsOutput(@TempDir Path temporaryDirectory) throws Exception {
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        project.failBuild();
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(5, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.FAILED);
        assertThat(result.standardOutput()).contains("fixture compilation failed");
        assertThat(result.standardError())
                .contains("[ERROR] src/main/java/example/Example.java:[4,9] cannot find symbol");
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.source())
                    .isEqualTo(project.root()
                            .resolve("src/main/java/example/Example.java")
                            .toUri());
            assertThat(diagnostic.diagnostic().message()).isEqualTo("cannot find symbol");
            assertThat(diagnostic.diagnostic().location()).isEqualTo("4:9");
        });
    }

    @Test
    void cancellationTerminatesTheCompleteWrapperProcessTree(@TempDir Path temporaryDirectory) throws Exception {
        assumeFalse(OperatingSystem.current() == OperatingSystem.WINDOWS);
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        project.blockBuild();
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());
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

    @Test
    void cancellingCleanBuildPreservesThePreviousCompiledOutput(@TempDir Path temporaryDirectory) throws Exception {
        assumeFalse(OperatingSystem.current() == OperatingSystem.WINDOWS);
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());
        adapter.start(new ProjectBuildRequest(6, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);
        String previousOutput = Files.readString(project.compiledOutput());
        project.blockBuild();
        ProjectBuildExecution execution = adapter.start(new ProjectBuildRequest(7, ProjectBuildKind.CLEAN));
        ProcessHandle child = project.awaitBlockingChild();

        try {
            assertThat(project.compiledOutput()).doesNotExist();

            execution.cancel();
            ProjectBuildResult result =
                    execution.completion().toCompletableFuture().get(5, SECONDS);

            assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.CANCELLED);
            assertThat(project.compiledOutput()).hasContent(previousOutput);
        } finally {
            child.destroyForcibly();
        }
    }

    @Test
    void failedCleanBuildPreservesThePreviousCompiledOutput(@TempDir Path temporaryDirectory) throws Exception {
        assumeFalse(OperatingSystem.current() == OperatingSystem.WINDOWS);
        MavenBuildTestProject project = MavenBuildTestProject.create(temporaryDirectory);
        MavenProjectBuildAdapter adapter = new MavenProjectBuildAdapter(project.root(), ForkJoinPool.commonPool());
        adapter.start(new ProjectBuildRequest(8, ProjectBuildKind.INCREMENTAL))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);
        String previousOutput = Files.readString(project.compiledOutput());
        project.failCleanBuild();

        ProjectBuildResult result = adapter.start(new ProjectBuildRequest(9, ProjectBuildKind.CLEAN))
                .completion()
                .toCompletableFuture()
                .get(5, SECONDS);

        assertThat(result.outcome()).isEqualTo(ProjectBuildOutcome.FAILED);
        assertThat(project.compiledOutput()).hasContent(previousOutput);
    }
}
