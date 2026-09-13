/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JdtLanguageServerDistributionTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void resolvesTheSingleLauncherAndHostConfiguration() throws IOException {
        Path launcher = createDistribution("config_mac_arm");

        JdtLanguageServerDistribution distribution =
                JdtLanguageServerDistribution.fromHome(temporaryDirectory, OperatingSystem.MACOS, "aarch64");

        assertThat(distribution.launcher()).isEqualTo(launcher);
        assertThat(distribution.platformConfiguration()).isEqualTo(temporaryDirectory.resolve("config_mac_arm"));
    }

    @Test
    void rejectsAnIncompleteDistribution() throws IOException {
        Files.createDirectories(temporaryDirectory.resolve("plugins"));
        Files.createDirectories(temporaryDirectory.resolve("config_linux"));

        assertThatIOException()
                .isThrownBy(() ->
                        JdtLanguageServerDistribution.fromHome(temporaryDirectory, OperatingSystem.LINUX, "amd64"))
                .withMessageContaining("Expected one Eclipse launcher");
    }

    private Path createDistribution(String configuration) throws IOException {
        Path plugins = Files.createDirectories(temporaryDirectory.resolve("plugins"));
        Files.createDirectories(temporaryDirectory.resolve(configuration));
        return Files.createFile(plugins.resolve("org.eclipse.equinox.launcher_1.7.0.jar"));
    }
}
