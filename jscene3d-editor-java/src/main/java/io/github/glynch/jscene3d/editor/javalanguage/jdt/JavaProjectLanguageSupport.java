/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

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

/** Applies the bundled JDT LS process to Maven and conventional Java projects. */
final class JavaProjectLanguageSupport implements EditorLanguageSupport {
    private static final System.Logger LOGGER = System.getLogger(JavaProjectLanguageSupport.class.getName());
    private static final EditorLanguageProjectSession INACTIVE_SESSION = () -> {};

    private final Optional<Path> distributionHome;
    private final JdtLanguageServerMetadata metadata;
    private final OperatingSystem operatingSystem;
    private final Executor executor;
    private final LanguageServerProcessLauncher launcher;

    JavaProjectLanguageSupport(
            Optional<Path> distributionHome,
            JdtLanguageServerMetadata metadata,
            OperatingSystem operatingSystem,
            Executor executor,
            LanguageServerProcessLauncher launcher) {
        this.distributionHome = Objects.requireNonNull(distributionHome, "distributionHome");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.launcher = Objects.requireNonNull(launcher, "launcher");
    }

    @Override
    public EditorLanguageProjectSession openProject(EditorProject project) {
        Path projectRoot = Path.of(Objects.requireNonNull(project, "project").root())
                .toAbsolutePath()
                .normalize();
        if (!isJavaProject(projectRoot)) {
            return INACTIVE_SESSION;
        }
        if (distributionHome.isEmpty()) {
            LOGGER.log(
                    System.Logger.Level.WARNING,
                    "Java project detected but the staged JDT LS distribution was not found");
            return INACTIVE_SESSION;
        }
        return new JdtLanguageProjectSession(
                projectRoot, distributionHome.orElseThrow(), metadata, operatingSystem, executor, launcher);
    }

    private static boolean isJavaProject(Path projectRoot) {
        return Files.isRegularFile(projectRoot.resolve("pom.xml"))
                || Files.isDirectory(projectRoot.resolve("src/main/java"))
                || Files.isDirectory(projectRoot.resolve("src/test/java"));
    }
}
