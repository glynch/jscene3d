/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

final class JavaLanguageModuleDescriptorTest {
    @Test
    void exportsOnlyTheJavaExtensionFacade() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.exports())
                .extracting(ModuleDescriptor.Exports::source)
                .containsExactly("io.github.glynch.jscene3d.editor.javalanguage");
    }

    @Test
    void reExportsTheEditorInterfaceUsedByTheFacade() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.editor.api"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
