/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the supported JPMS interface and project-import provider declaration. */
final class DoomModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInDoomModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.doom");
    }

    /** Exports only decoding interfaces, retains transitive WAD types, and provides import. */
    @Test
    void declaresSupportedModuleInterface() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.doom.diagnostic", "io.github.glynch.jscene3d.doom.map");
        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.wad"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
        assertThat(descriptor.provides()).singleElement().satisfies(provider -> {
            assertThat(provider.service()).isEqualTo(ProjectImportExtension.class.getName());
            assertThat(provider.providers())
                    .containsExactly("io.github.glynch.jscene3d.doom.importing.internal.DoomImportExtension");
        });
    }
}
