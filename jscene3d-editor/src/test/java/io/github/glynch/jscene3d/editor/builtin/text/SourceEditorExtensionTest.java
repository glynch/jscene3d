/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.builtin.image.ImageViewerExtension;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.file.EditorFileKind;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.net.URI;
import org.junit.jupiter.api.Test;

final class SourceEditorExtensionTest {
    @Test
    void contributesBuiltInSourceAndImageAssociations() {
        try (EditorExtensionHost host =
                new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext())) {
            host.activate(new SourceEditorExtension());
            host.activate(new ImageViewerExtension());

            assertThat(host.resolveFileType(URI.create("file:///workspace/src/Player.java")))
                    .get()
                    .satisfies(type -> {
                        assertThat(type.kind()).isEqualTo(EditorFileKind.TEXT);
                        assertThat(type.language()).contains(EditorLanguages.JAVA);
                        assertThat(type.icon().id()).isEqualTo(EditorIcons.JAVA);
                    });
            assertThat(host.resolveFileType(URI.create("file:///workspace/pom.xml")))
                    .get()
                    .satisfies(type -> {
                        assertThat(type.language()).contains(EditorLanguages.XML);
                        assertThat(type.icon().id()).isEqualTo(EditorIcons.MAVEN);
                    });
            assertThat(host.resolveFileType(URI.create("file:///workspace/schema.xsd")))
                    .get()
                    .extracting(type -> type.icon().id())
                    .isEqualTo(EditorIcons.XML);
            assertThat(host.resolveFileType(URI.create("file:///workspace/preview.png")))
                    .get()
                    .satisfies(type -> {
                        assertThat(type.kind()).isEqualTo(EditorFileKind.IMAGE);
                        assertThat(type.language()).isEmpty();
                    });
            assertThat(host.resolveFileType(URI.create("file:///workspace/README.md")))
                    .isEmpty();
        }
    }
}
