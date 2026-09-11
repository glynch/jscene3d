/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

final class EditorApiModuleTest {
    @Test
    void exportsTheEditorInterfaceWithoutDependingOnJavaFx() {
        ModuleDescriptor descriptor = EditorView.class.getModule().getDescriptor();

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.name()).isEqualTo("io.github.glynch.jscene3d.editor.api");
        assertThat(descriptor.exports())
                .extracting(ModuleDescriptor.Exports::source)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.editor.command",
                        "io.github.glynch.jscene3d.editor.diagnostic",
                        "io.github.glynch.jscene3d.editor.extension",
                        "io.github.glynch.jscene3d.editor.lifecycle",
                        "io.github.glynch.jscene3d.editor.project",
                        "io.github.glynch.jscene3d.editor.status",
                        "io.github.glynch.jscene3d.editor.view",
                        "io.github.glynch.jscene3d.editor.window");
        assertThat(descriptor.requires())
                .extracting(ModuleDescriptor.Requires::name)
                .noneMatch(name -> name.startsWith("javafx."));
    }
}
