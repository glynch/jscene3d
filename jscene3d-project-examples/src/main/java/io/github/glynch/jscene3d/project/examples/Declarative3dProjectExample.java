/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import io.github.glynch.jscene3d.examples.framework.ExampleContext;
import io.github.glynch.jscene3d.examples.framework.ExampleFrame;
import io.github.glynch.jscene3d.examples.framework.ExampleLauncher;
import io.github.glynch.jscene3d.examples.framework.HostedExample;
import io.github.glynch.jscene3d.game.GameRuntime;
import io.github.glynch.jscene3d.game.input.ActionSnapshot;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoadResult;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoader;
import io.github.glynch.jscene3d.project.runtime.lwjgl.JScene3dRuntimeExtension;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Runs the engine-owned declarative 3d project fixture in a native window. */
public final class Declarative3dProjectExample {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";

    /** Prevents instantiation of this application entry point. */
    private Declarative3dProjectExample() {
        throw new AssertionError("Declarative3dProjectExample cannot be instantiated");
    }

    /**
     * Loads the project directory supplied by the Maven run profile.
     *
     * @param arguments one project-directory path
     */
    public static void main(String[] arguments) {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("expected one project-directory path");
        }
        Path projectDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        ExampleLauncher.launch("JScene3D - Declarative 3d Project", context -> openProject(context, projectDirectory));
    }

    /** Opens and starts one declarative project inside the example host. */
    private static HostedExample openProject(ExampleContext context, Path projectDirectory) {
        ProjectLoadResult projectResult = new ProjectLoader(ENGINE_VERSION).load(projectDirectory);
        GameProject project = requireValue(projectResult.project(), projectResult.diagnostics(), "project");
        ProjectRuntimeLoadResult runtimeResult = new ProjectRuntimeLoader(ENGINE_VERSION)
                .load(
                        project,
                        Declarative3dProjectExample.class.getClassLoader(),
                        List.of(new JScene3dRuntimeExtension(context.window(), context.renderer())));
        ProjectRuntime projectRuntime = requireValue(runtimeResult.runtime(), runtimeResult.diagnostics(), "runtime");
        GameRuntime gameRuntime = new GameRuntime(projectRuntime);
        gameRuntime.start();
        return new ProjectExample(gameRuntime);
    }

    /** Requires one successful loading result while preserving its structured diagnostics. */
    private static <T> T requireValue(Optional<T> value, List<ProjectDiagnostic> diagnostics, String description) {
        return value.orElseThrow(() -> new IllegalStateException(description + " could not be loaded: " + diagnostics));
    }

    /** Hosted lifecycle adapter for one running project. */
    private static final class ProjectExample implements HostedExample {
        private final GameRuntime runtime;

        /** Stores one started runtime. */
        private ProjectExample(GameRuntime runtime) {
            this.runtime = runtime;
        }

        @Override
        public void resize() {
            // The render host reads the current framebuffer aspect ratio each frame.
        }

        @Override
        public void update(ExampleFrame frame) {
            long elapsedNanos = Math.round(frame.elapsedSeconds() * 1_000_000_000.0);
            runtime.advance(Duration.ofNanos(elapsedNanos), ActionSnapshot.empty());
        }

        @Override
        public void render() {
            runtime.render();
        }

        @Override
        public void close() {
            runtime.close();
        }
    }
}
