/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JdtLanguageServerProjectLayoutTest {
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

        JdtLanguageServerProjectLayout layout = JdtLanguageServerProjectLayout.prepare(
                project, distribution, new JdtLanguageServerMetadata("1.61.0", "archive", "sha", "source"));

        assertThat(layout.configuration().resolve("config.ini")).hasContent("configuration");
        assertThat(layout.workspace()).isDirectory();
        assertThat(layout.errorLog().getParent()).isDirectory();
        assertThat(layout.workspace()).startsWith(project.resolve(".jscene3d/cache"));
    }
}
