/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.lsp.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

final class LanguageServerModuleDescriptorTest {
    @Test
    void exportsOnlyTheClientAndProcessLifecycleSeams() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.exports())
                .extracting(ModuleDescriptor.Exports::source)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.editor.lsp.client", "io.github.glynch.jscene3d.editor.lsp.process");
    }

    @Test
    void resolvesTheJsonRuntimeModules() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .extracting(ModuleDescriptor.Requires::name)
                .contains("com.google.gson", "org.eclipse.lsp4j", "org.eclipse.lsp4j.jsonrpc");
    }

    @Test
    void reExportsLsp4jTypesUsedByTheClientApi() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("org.eclipse.lsp4j"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }

    @Test
    void reExportsEditorApiTypesUsedByTheClientApi() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.editor.api"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
