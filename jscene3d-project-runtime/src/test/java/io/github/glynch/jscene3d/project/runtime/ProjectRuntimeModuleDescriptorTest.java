/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.project.runtime.extension.ProjectRuntimeExtension;
import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the executable project runtime's named-module boundary. */
final class ProjectRuntimeModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInProjectRuntimeModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.project.runtime");
    }

    /** Exposes only the runtime seam and trusted extension contracts. */
    @Test
    void exportsSupportedPackagesAndDeclaresServiceUse() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.project.runtime",
                        "io.github.glynch.jscene3d.project.runtime.extension");
        assertThat(descriptor.uses()).containsExactly(ProjectRuntimeExtension.class.getName());
    }
}
