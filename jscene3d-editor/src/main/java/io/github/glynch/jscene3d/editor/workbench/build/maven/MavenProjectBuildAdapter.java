/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildKind;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildRequest;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildAdapter;
import io.github.glynch.jscene3d.editor.workbench.build.execution.ProjectBuildExecution;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Executes development builds through a Maven project's own wrapper. */
public final class MavenProjectBuildAdapter implements ProjectBuildAdapter {
    private final Path projectRoot;
    private final OperatingSystem operatingSystem;

    /**
     * Creates an adapter for a Maven project on the current operating system.
     *
     * @param projectRoot directory containing the Maven project descriptor
     */
    public MavenProjectBuildAdapter(Path projectRoot) {
        this(projectRoot, OperatingSystem.current());
    }

    /**
     * Reports whether a workspace root contains a Maven project descriptor.
     *
     * @param projectRoot candidate workspace root
     * @return whether Maven can represent the project build
     */
    public static boolean supports(Path projectRoot) {
        Path root = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        return Files.isRegularFile(root.resolve("pom.xml"));
    }

    MavenProjectBuildAdapter(Path projectRoot, OperatingSystem operatingSystem) {
        this.projectRoot = Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
        this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
    }

    @Override
    public ProjectBuildExecution start(ProjectBuildRequest request) {
        Objects.requireNonNull(request, "request");
        List<String> command = command(request.kind());
        try {
            if (request.kind() == ProjectBuildKind.CLEAN) {
                return startRebuild(command);
            }
            return MavenBuildExecution.start(projectRoot, command);
        } catch (IOException failure) {
            throw new UncheckedIOException("could not start Maven wrapper in " + projectRoot, failure);
        }
    }

    private ProjectBuildExecution startRebuild(List<String> command) throws IOException {
        MavenBuildOutputBackup backup = MavenBuildOutputBackup.capture(projectRoot);
        try {
            return new MavenRebuildExecution(MavenBuildExecution.start(projectRoot, command), backup);
        } catch (IOException | RuntimeException failure) {
            try {
                backup.rollback();
            } catch (IOException rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }

    private List<String> command(ProjectBuildKind kind) {
        Path wrapper = wrapper();
        List<String> command = new ArrayList<>();
        if (operatingSystem == OperatingSystem.WINDOWS) {
            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");
        }
        command.add(wrapper.toString());
        if (kind == ProjectBuildKind.CLEAN) {
            command.add("clean");
        }
        command.add("process-classes");
        return List.copyOf(command);
    }

    private Path wrapper() {
        Path wrapper = projectRoot.resolve(operatingSystem == OperatingSystem.WINDOWS ? "mvnw.cmd" : "mvnw");
        if (!Files.isRegularFile(wrapper)) {
            throw new IllegalStateException("Maven wrapper not found: " + wrapper);
        }
        return wrapper;
    }
}
