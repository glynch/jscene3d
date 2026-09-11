/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Verifies installation and semantic foundations of the packaged editor theme. */
final class EditorThemeTest {
    /** Keeps the accepted semantic colour roles in the packaged stylesheet. */
    @Test
    void declaresSemanticColourRoles() throws IOException {
        String stylesheet;
        try (InputStream input =
                Objects.requireNonNull(EditorTheme.class.getResourceAsStream("editor.css"), "editor.css")) {
            stylesheet = new String(input.readAllBytes(), UTF_8);
        }

        assertThat(stylesheet)
                .contains(
                        "-jscene-canvas:",
                        "-jscene-chrome:",
                        "-jscene-panel:",
                        "-jscene-selected:",
                        "-jscene-accent:",
                        "-jscene-focus:",
                        "-jscene-divider:",
                        "-jscene-text-strong:",
                        "-jscene-text:",
                        "-jscene-text-muted:",
                        "-jscene-success:",
                        "-jscene-warning:",
                        "-jscene-error:");
    }
}
