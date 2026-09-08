/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

/** Verifies that the editor remains a named application module with no broad public export. */
final class EditorModuleDescriptorTest {

    @Test
    void runsTestsInTheEditorModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.editor");
    }

    @Test
    void exposesItsApplicationClassOnlyToJavaFx() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.exports())
                .singleElement()
                .returns("io.github.glynch.jscene3d.editor", ModuleDescriptor.Exports::source)
                .returns(true, ModuleDescriptor.Exports::isQualified)
                .satisfies(export -> assertThat(export.targets()).containsExactly("javafx.graphics"));
    }
}
