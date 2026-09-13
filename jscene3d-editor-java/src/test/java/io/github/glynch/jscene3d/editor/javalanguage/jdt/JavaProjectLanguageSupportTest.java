/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessLauncher;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        List<JdtLanguageServerStatus> statuses = new ArrayList<>();
        JavaProjectLanguageSupport support = support(executor, statuses);

        EditorLanguageProjectSession session = support.openProject(project(projectRoot));

        assertThat(executor.tasks).hasSize(1);
        assertThat(statuses).containsExactly(JdtLanguageServerStatus.starting());
        session.close();
        assertThat(statuses).containsExactly(JdtLanguageServerStatus.starting(), JdtLanguageServerStatus.inactive());
    }

    @Test
    void doesNotScheduleJdtLsForANonJavaProject() throws IOException {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        QueuedExecutor executor = new QueuedExecutor();
        List<JdtLanguageServerStatus> statuses = new ArrayList<>();
        JavaProjectLanguageSupport support = support(executor, statuses);

        try (EditorLanguageProjectSession session = support.openProject(project(projectRoot))) {
            assertThat(executor.tasks).isEmpty();
            assertThat(statuses).containsExactly(JdtLanguageServerStatus.inactive());
        }
    }

    @Test
    void reportsMissingDistributionForAJavaProject() throws IOException {
        Path projectRoot = Files.createDirectories(temporaryDirectory.resolve("project"));
        Files.writeString(projectRoot.resolve("pom.xml"), "<project/>");
        List<JdtLanguageServerStatus> statuses = new ArrayList<>();
        JavaProjectLanguageSupport support = new JavaProjectLanguageSupport(
                Optional.empty(), configuration(Runnable::run), statuses::add, diagnostics());

        try (EditorLanguageProjectSession ignored = support.openProject(project(projectRoot))) {
            assertThat(statuses)
                    .containsExactly(JdtLanguageServerStatus.failed("Bundled JDT LS distribution was not found"));
        }
    }

    private JavaProjectLanguageSupport support(Executor executor, List<JdtLanguageServerStatus> statuses) {
        return new JavaProjectLanguageSupport(
                Optional.of(temporaryDirectory.resolve("distribution")),
                configuration(executor),
                statuses::add,
                diagnostics());
    }

    private JavaProjectLanguageSupport.Configuration configuration(Executor executor) {
        return new JavaProjectLanguageSupport.Configuration(
                temporaryDirectory.resolve("cache"),
                new JdtLanguageServerMetadata("1.61.0", "archive", "sha", "source"),
                OperatingSystem.MACOS,
                executor,
                new LanguageServerProcessLauncher(executor),
                "test");
    }

    private static EditorProject project(Path root) {
        return new EditorProject("project", "Project", root.toUri());
    }

    private static EditorDiagnosticCollection diagnostics() {
        Map<URI, List<EditorDiagnostic>> published = new HashMap<>();
        return new EditorDiagnosticCollection() {
            @Override
            public DiagnosticCollectionId id() {
                return new DiagnosticCollectionId("io.github.glynch.test.java");
            }

            @Override
            public void replace(URI source, List<EditorDiagnostic> diagnostics) {
                published.put(source, List.copyOf(diagnostics));
            }

            @Override
            public void replaceAll(Map<URI, List<EditorDiagnostic>> diagnostics) {
                published.clear();
                diagnostics.forEach((source, values) -> published.put(source, List.copyOf(values)));
            }

            @Override
            public void clear(URI source) {
                published.remove(source);
            }

            @Override
            public void clear() {
                published.clear();
            }

            @Override
            public void close() {
                published.clear();
            }
        };
    }

    private static final class QueuedExecutor implements Executor {
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }
    }
}
