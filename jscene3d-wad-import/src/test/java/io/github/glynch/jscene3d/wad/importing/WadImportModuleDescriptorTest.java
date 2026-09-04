/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad.importing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the WAD import adapter's named-module boundary. */
final class WadImportModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInWadImportModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.wad.importing");
    }

    /** Exports only the extension seam and declares its service provider. */
    @Test
    void exportsOnlyExtensionPackageAndProvidesService() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.wad.importing");
        assertThat(descriptor.provides()).singleElement().satisfies(provider -> {
            assertThat(provider.service()).isEqualTo(ProjectImportExtension.class.getName());
            assertThat(provider.providers()).containsExactly(WadImportExtension.class.getName());
        });
        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.project.importing"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
