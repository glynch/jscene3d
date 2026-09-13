/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JdtLanguageServerProjectLayoutTest {
    private static final JdtLanguageServerMetadata METADATA =
            new JdtLanguageServerMetadata("1.61.0", "archive", "sha", "source");

    @TempDir
    private Path temporaryDirectory;

    @Test
    void stagesWritableConfigurationAndVersionedProjectData() throws IOException {
        Path distributionHome = Files.createDirectories(temporaryDirectory.resolve("distribution"));
        Path sourceConfiguration = Files.createDirectories(distributionHome.resolve("config_mac"));
        Files.writeString(sourceConfiguration.resolve("config.ini"), "configuration");
        Path launcher = Files.createFile(distributionHome.resolve("launcher.jar"));
        JdtLanguageServerDistribution distribution =
                new JdtLanguageServerDistribution(distributionHome, launcher, sourceConfiguration);
        Path project = Files.createDirectories(temporaryDirectory.resolve("project"));
        Path userCache = temporaryDirectory.resolve("user-cache");

        JdtLanguageServerProjectLayout layout =
                JdtLanguageServerProjectLayout.prepare(project, userCache, distribution, METADATA);

        assertThat(layout.configuration().resolve("config.ini")).hasContent("configuration");
        assertThat(layout.workspace()).isDirectory();
        assertThat(layout.errorLog().getParent()).isDirectory();
        assertThat(layout.workspace()).startsWith(userCache);
        assertThat(layout.workspace().startsWith(project)).isFalse();
    }

    @Test
    void givesProjectsWithTheSameNameDistinctPersistentWorkspaces() throws IOException {
        Path distributionHome = Files.createDirectories(temporaryDirectory.resolve("distribution"));
        Path sourceConfiguration = Files.createDirectories(distributionHome.resolve("config_mac"));
        Files.writeString(sourceConfiguration.resolve("config.ini"), "configuration");
        Path launcher = Files.createFile(distributionHome.resolve("launcher.jar"));
        JdtLanguageServerDistribution distribution =
                new JdtLanguageServerDistribution(distributionHome, launcher, sourceConfiguration);
        Path firstProject = Files.createDirectories(temporaryDirectory.resolve("first/project"));
        Path secondProject = Files.createDirectories(temporaryDirectory.resolve("second/project"));
        Path userCache = temporaryDirectory.resolve("user-cache");

        JdtLanguageServerProjectLayout first =
                JdtLanguageServerProjectLayout.prepare(firstProject, userCache, distribution, METADATA);
        JdtLanguageServerProjectLayout second =
                JdtLanguageServerProjectLayout.prepare(secondProject, userCache, distribution, METADATA);

        assertThat(first.workspace()).isNotEqualTo(second.workspace());
    }

    @Test
    void rejectsAWorkspaceCacheNestedInsideTheJavaProject() throws IOException {
        Path distributionHome = Files.createDirectories(temporaryDirectory.resolve("distribution"));
        Path sourceConfiguration = Files.createDirectories(distributionHome.resolve("config_mac"));
        Files.writeString(sourceConfiguration.resolve("config.ini"), "configuration");
        Path launcher = Files.createFile(distributionHome.resolve("launcher.jar"));
        JdtLanguageServerDistribution distribution =
                new JdtLanguageServerDistribution(distributionHome, launcher, sourceConfiguration);
        Path project = Files.createDirectories(temporaryDirectory.resolve("project"));

        assertThatIOException()
                .isThrownBy(() -> JdtLanguageServerProjectLayout.prepare(
                        project, project.resolve(".jscene3d/cache"), distribution, METADATA))
                .withMessageContaining("must not overlap");
    }
}
