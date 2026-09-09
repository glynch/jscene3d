/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import java.lang.module.ModuleDescriptor;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the supported JPMS interface and project-import provider declaration. */
final class DoomModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInDoomModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.doom");
    }

    /** Exports reusable Doom interfaces with their public dependencies and provides the extension seams. */
    @Test
    void declaresSupportedModuleInterface() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.doom.diagnostic",
                        "io.github.glynch.jscene3d.doom.geometry",
                        "io.github.glynch.jscene3d.doom.map",
                        "io.github.glynch.jscene3d.doom.material",
                        "io.github.glynch.jscene3d.doom.runtime");
        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.wad"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals("io.github.glynch.jscene3d.project"))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
        Map<String, ModuleDescriptor.Provides> providers = descriptor.provides().stream()
                .collect(Collectors.toUnmodifiableMap(ModuleDescriptor.Provides::service, Function.identity()));
        assertThat(providers)
                .containsOnlyKeys(ProjectImportExtension.class.getName(), ComponentRuntimeExtension.class.getName());
        assertThat(providers.get(ProjectImportExtension.class.getName()).providers())
                .containsExactly("io.github.glynch.jscene3d.doom.importing.internal.DoomImportExtension");
        assertThat(providers.get(ComponentRuntimeExtension.class.getName()).providers())
                .containsExactly("io.github.glynch.jscene3d.doom.runtime.internal.DoomRuntimeExtension");
    }
}
