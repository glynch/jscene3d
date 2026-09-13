/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MonacoResourcesTest {
    @Test
    void stagesThePinnedDistributionForJavaFxWebKit() throws Exception {
        Path loader = Path.of(MonacoResources.loader().toURI());
        Path editorPage = Path.of(MonacoResources.editorPage().toURI());

        assertThat(MonacoResources.loader().getProtocol()).isEqualTo("file");
        assertThat(loader).isRegularFile();
        assertThat(Files.readString(loader)).contains("define.amd");
        assertThat(editorPage).isRegularFile();
        assertThat(MonacoResources.baseUrl())
                .isEqualTo(loader.getParent().toUri().toString());
        assertThat(loader.getParent().getFileName().toString()).contains(MonacoResources.VERSION);
    }
}
