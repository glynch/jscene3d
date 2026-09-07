/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class DesktopModuleDescriptorTest {
    @Test
    void runsTestsInTheDesktopModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.project.desktop");
    }

    @Test
    void exportsOnlyTheDesktopHostPackage() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.project.desktop");
        assertTransitiveRequirement(descriptor, "io.github.glynch.jscene3d.project");
        assertTransitiveRequirement(descriptor, "io.github.glynch.jscene3d.project.runtime");
    }

    /** Verifies that one public-signature dependency is re-exported to desktop-host clients. */
    private static void assertTransitiveRequirement(ModuleDescriptor descriptor, String moduleName) {
        assertThat(descriptor.requires())
                .filteredOn(requirement -> requirement.name().equals(moduleName))
                .singleElement()
                .satisfies(requirement ->
                        assertThat(requirement.modifiers()).contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE));
    }
}
