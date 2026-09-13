/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LanguageServerProcessConfigurationTest {
    @Test
    void copiesTheCommandAndNormalizesPaths() {
        List<String> command = new ArrayList<>(List.of("java", "-version"));
        Path root = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();

        LanguageServerProcessConfiguration configuration =
                new LanguageServerProcessConfiguration(command, root.resolve("work/.."), root.resolve("logs/jdt.log"));
        command.add("changed");

        assertThat(configuration.command()).containsExactly("java", "-version");
        assertThat(configuration.workingDirectory()).isEqualTo(root);
    }

    @Test
    void rejectsIncompleteOrRelativeConfiguration() {
        Path absolute = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LanguageServerProcessConfiguration(List.of(), absolute, absolute.resolve("log")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LanguageServerProcessConfiguration(
                        List.of("java"), Path.of("relative"), absolute.resolve("log")));
    }
}
