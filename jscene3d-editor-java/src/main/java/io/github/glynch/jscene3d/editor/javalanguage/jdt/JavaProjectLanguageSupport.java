/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.language.EditorLanguageProjectSession;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupport;
import io.github.glynch.jscene3d.editor.lsp.process.LanguageServerProcessLauncher;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Applies the bundled JDT LS process to Maven and conventional Java projects. */
final class JavaProjectLanguageSupport implements EditorLanguageSupport {
    private static final System.Logger LOGGER = System.getLogger(JavaProjectLanguageSupport.class.getName());
    private final Optional<Path> distributionHome;
    private final Configuration configuration;
    private final Consumer<JdtLanguageServerStatus> status;
    private final EditorDiagnosticCollection diagnostics;

    JavaProjectLanguageSupport(
            Optional<Path> distributionHome,
            Configuration configuration,
            Consumer<JdtLanguageServerStatus> status,
            EditorDiagnosticCollection diagnostics) {
        this.distributionHome = Objects.requireNonNull(distributionHome, "distributionHome");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.status = Objects.requireNonNull(status, "status");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public EditorLanguageProjectSession openProject(EditorProject project) {
        Path projectRoot = Path.of(Objects.requireNonNull(project, "project").root())
                .toAbsolutePath()
                .normalize();
        if (!isJavaProject(projectRoot)) {
            status.accept(JdtLanguageServerStatus.inactive());
            return inactiveSession();
        }
        if (distributionHome.isEmpty()) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Java project detected but the staged JDT LS distribution was not found");
            status.accept(JdtLanguageServerStatus.failed("Bundled JDT LS distribution was not found"));
            return inactiveSession();
        }
        status.accept(JdtLanguageServerStatus.starting());
        return new JdtLanguageProjectSession(
                projectRoot,
                project.name(),
                status,
                new JdtDiagnosticPublisher(diagnostics),
                new JdtLanguageProjectSession.Runtime(
                        distributionHome.orElseThrow(),
                        configuration.cacheRoot(),
                        configuration.metadata(),
                        configuration.operatingSystem(),
                        configuration.executor(),
                        configuration.launcher(),
                        configuration.clientVersion()));
    }

    private static boolean isJavaProject(Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("pom.xml"))
                || Files.isDirectory(projectRoot.resolve("src/main/java"))
                || Files.isDirectory(projectRoot.resolve("src/test/java"));
    }

    private EditorLanguageProjectSession inactiveSession() {
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                status.accept(JdtLanguageServerStatus.inactive());
            }
        };
    }

    record Configuration(
            Path cacheRoot,
            JdtLanguageServerMetadata metadata,
            OperatingSystem operatingSystem,
            Executor executor,
            LanguageServerProcessLauncher launcher,
            String clientVersion) {
        Configuration {
            Objects.requireNonNull(cacheRoot, "cacheRoot");
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(operatingSystem, "operatingSystem");
            Objects.requireNonNull(executor, "executor");
            Objects.requireNonNull(launcher, "launcher");
            Objects.requireNonNull(clientVersion, "clientVersion");
        }
    }
}
