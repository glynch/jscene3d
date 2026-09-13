/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorExtensionCatalogTest {
    @Test
    void publishesInstalledExtensionMetadata() {
        EditorExtensionCatalog catalog = new EditorExtensionCatalog();
        List<List<EditorExtensionDescriptor>> snapshots = new ArrayList<>();
        catalog.observe(snapshots::add);
        catalog.add(new EditorExtensionDescriptor(
                "io.github.glynch.test.catalogued",
                "Catalogued",
                "Test extension metadata.",
                "Tests",
                Optional.of("1.2.3"),
                false));

        assertThat(catalog.installed())
                .singleElement()
                .extracting(EditorExtensionDescriptor::displayName)
                .isEqualTo("Catalogued");
        assertThat(snapshots.getLast()).isEqualTo(catalog.installed());

        catalog.close();
        assertThat(snapshots.getLast()).isEmpty();
    }
}
