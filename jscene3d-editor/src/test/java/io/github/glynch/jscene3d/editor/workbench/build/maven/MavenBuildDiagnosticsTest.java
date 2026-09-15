/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.maven;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies bounded parsing of Maven compiler diagnostics. */
final class MavenBuildDiagnosticsTest {
    @Test
    void parsesWarningsAndIgnoresMalformedCompilerOutput(@TempDir Path projectRoot) {
        String output = """
                [WARNING] src/main/java/example/Example.java:[2,3] deprecated API use
                [ERROR] not-a-source-location
                [ERROR] src/main/java/example/Example.java:[not,a-number] invalid location
                """;

        assertThat(MavenBuildDiagnostics.parse(projectRoot, output, ""))
                .singleElement()
                .satisfies(diagnostic -> {
                    assertThat(diagnostic.source())
                            .isEqualTo(projectRoot
                                    .resolve("src/main/java/example/Example.java")
                                    .toUri());
                    assertThat(diagnostic.diagnostic().severity()).isEqualTo(EditorDiagnosticSeverity.WARNING);
                    assertThat(diagnostic.diagnostic().location()).isEqualTo("2:3");
                });
    }
}
