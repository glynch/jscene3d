/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.lsp.LanguageServerProcessLauncher;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaProjectLanguageSupportTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void schedulesJdtLsForAMavenProjectWithoutBlockingTheCaller() throws IOException {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>");
        QueuedExecutor executor = new QueuedExecutor();
        JavaProjectLanguageSupport support = support(executor);

        EditorLanguageProjectSession session = support.openProject(project(projectRoot));

        assertThat(executor.tasks).hasSize(1);
        session.close();
    }

    @Test
    void doesNotScheduleJdtLsForANonJavaProject() throws IOException {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        QueuedExecutor executor = new QueuedExecutor();
        JavaProjectLanguageSupport support = support(executor);

        try (EditorLanguageProjectSession session = support.openProject(project(projectRoot))) {
            assertThat(executor.tasks).isEmpty();
        }
    }

    private JavaProjectLanguageSupport support(Executor executor) {
        return new JavaProjectLanguageSupport(
                Optional.of(temporaryDirectory.resolve("distribution")),
                new JdtLanguageServerMetadata("1.61.0", "archive", "sha", "source"),
                OperatingSystem.MACOS,
                executor,
                new LanguageServerProcessLauncher(executor));
    }

    private static EditorProject project(Path root) {
        return new EditorProject("project", "Project", root.toUri());
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }
    }
}
