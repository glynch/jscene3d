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
    void exportsOnlyTheProcessLifecycleSeam() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.exports())
                .extracting(ModuleDescriptor.Exports::source)
                .containsExactly("io.github.glynch.jscene3d.editor.lsp.process");
    }
}
