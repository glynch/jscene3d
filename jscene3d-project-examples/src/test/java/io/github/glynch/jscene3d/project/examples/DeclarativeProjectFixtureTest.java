/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.examples;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.manifest.ProjectLoadResult;
import io.github.glynch.jscene3d.project.manifest.ProjectLoader;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntime;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoadResult;
import io.github.glynch.jscene3d.project.runtime.ProjectRuntimeLoader;
import io.github.glynch.jscene3d.project.runtime.scene3d.JScene3dRuntimeExtension;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies that the runnable project fixture uses the supported declarative runtime interface. */
final class DeclarativeProjectFixtureTest {
    private static final String ENGINE_VERSION = "0.1.0-SNAPSHOT";

    /** Loads, starts, and closes the fixture with its declarative static collision. */
    @Test
    void loadsDeclarativeProjectFixture() {
        Path fixtureRoot = Path.of("src/main/project").toAbsolutePath().normalize();
        ProjectLoadResult projectResult = new ProjectLoader(ENGINE_VERSION).load(fixtureRoot);
        assertThat(projectResult.diagnostics()).isEmpty();
        GameProject loadedProject = projectResult.project().orElseThrow();
        JScene3dRuntimeExtension extension = JScene3dRuntimeExtension.headless();
        ProjectRuntimeLoadResult runtimeResult = new ProjectRuntimeLoader(ENGINE_VERSION)
                .load(loadedProject, getClass().getClassLoader(), List.of(extension));
        assertThat(runtimeResult.diagnostics()).isEmpty();

        ProjectRuntime runtime = runtimeResult.runtime().orElseThrow();
        assertThat(extension.physicsWorld().collisionObjectCount()).isOne();
        assertThat(extension.physicsWorld().colliderCount()).isOne();
        runtime.start();
        runtime.close();

        assertThat(extension.physicsWorld().collisionObjectCount()).isZero();
        assertThat(extension.physicsWorld().colliderCount()).isZero();
    }
}
