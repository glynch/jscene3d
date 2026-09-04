/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.importing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.importing.extension.ProjectImportExtension;
import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the deterministic project import named-module seam. */
final class ProjectImportModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInProjectImportModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.project.importing");
    }

    /** Exposes only orchestration values and trusted adapter contracts. */
    @Test
    void exportsSupportedPackagesAndDeclaresServiceUse() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.project.importing",
                        "io.github.glynch.jscene3d.project.importing.extension");
        assertThat(descriptor.uses()).containsExactly(ProjectImportExtension.class.getName());
    }
}
