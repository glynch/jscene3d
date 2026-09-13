/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.lsp.LanguageServerProcessConfiguration;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JdtLanguageServerCommandTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void buildsTheDocumentedDirectJdtLsCommand() {
        Path project = temporaryDirectory.resolve("project");
        Path launcher = temporaryDirectory.resolve("distribution/plugins/launcher.jar");
        Path configuration = temporaryDirectory.resolve("cache/configuration");
        Path workspace = temporaryDirectory.resolve("cache/workspace");
        Path log = temporaryDirectory.resolve("cache/logs/stderr.log");
        JdtLanguageServerDistribution distribution =
                new JdtLanguageServerDistribution(temporaryDirectory.resolve("distribution"), launcher, configuration);
        JdtLanguageServerProjectLayout layout = new JdtLanguageServerProjectLayout(configuration, workspace, log);

        LanguageServerProcessConfiguration process =
                JdtLanguageServerCommand.create(project, distribution, layout, OperatingSystem.MACOS);

        assertThat(process.command())
                .containsSubsequence("-jar", launcher.toString())
                .containsSubsequence("-configuration", configuration.toString())
                .containsSubsequence("-data", workspace.toString());
        assertThat(process.workingDirectory()).isEqualTo(project);
        assertThat(process.errorLog()).isEqualTo(log);
    }
}
