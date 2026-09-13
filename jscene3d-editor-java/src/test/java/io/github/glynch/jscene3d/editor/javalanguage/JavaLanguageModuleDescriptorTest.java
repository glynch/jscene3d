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

    @Test
    void opensItsMessageCataloguePackageOnlyToI18n() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.opens())
                .anySatisfy(openedPackage -> {
                    assertThat(openedPackage.source()).isEqualTo("io.github.glynch.jscene3d.editor.javalanguage");
                    assertThat(openedPackage.targets()).containsExactly("io.github.glynch.jscene3d.i18n");
                })
                .anySatisfy(openedPackage -> {
                    assertThat(openedPackage.source()).isEqualTo("io.github.glynch.jscene3d.editor.javalanguage.jdt");
                    assertThat(openedPackage.targets())
                            .containsExactlyInAnyOrder("com.google.gson", "org.eclipse.lsp4j.jsonrpc");
                });
    }
}
