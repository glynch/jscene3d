/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.standard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.extension.PropertyDescriptorKeys;
import io.github.glynch.jscene3d.project.extension.PropertyEditorSemantics;
import io.github.glynch.jscene3d.project.standard.spatial3d.StandardSpatial3dDescriptors;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import org.junit.jupiter.api.Test;

/** Verifies the safe, authoritative built-in descriptor catalog. */
final class StandardProjectDescriptorsTest {
    /** Publishes every built-in descriptor once and in dependency order. */
    @Test
    void publishesBuiltInDescriptors() {
        assertThat(StandardProjectDescriptors.all())
                .extracting(ExtensionDescriptor::id)
                .containsExactly(
                        "io.github.glynch.jscene3d.application",
                        "io.github.glynch.jscene3d.spatial3d",
                        "io.github.glynch.jscene3d.physics3d",
                        "io.github.glynch.jscene3d.game3d",
                        "io.github.glynch.jscene3d.presentation");
    }

    /** Retains editor semantics in safe descriptor metadata. */
    @Test
    void publishesEditorSemantics() {
        var transform =
                StandardSpatial3dDescriptors.extensionDescriptor().components().getFirst();

        assertThat(transform
                        .properties()
                        .get(StandardSpatial3dDescriptors.positionProperty())
                        .editorMetadata())
                .containsEntry(
                        PropertyDescriptorKeys.EDITOR_SEMANTIC,
                        new ProjectValue.TextValue(PropertyEditorSemantics.VECTOR3));
    }
}
