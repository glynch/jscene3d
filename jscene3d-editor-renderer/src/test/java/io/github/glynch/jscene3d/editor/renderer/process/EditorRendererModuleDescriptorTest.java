/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the renderer process exposes only its stable protocol vocabulary. */
final class EditorRendererModuleDescriptorTest {
    @Test
    void exportsOnlyRendererProtocol() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.editor.renderer");
        assertThat(exports)
                .containsExactly("io.github.glynch.jscene3d.editor.renderer.protocol")
                .doesNotContain("io.github.glynch.jscene3d.editor.renderer.process");
    }

    @Test
    void keepsIosurfaceAsAnImplementationDependency() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.iosurface.macos"))
                .singleElement()
                .satisfies(requirement -> assertThat(requirement.modifiers())
                        .doesNotContain(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
