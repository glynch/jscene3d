/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the named-module boundary exposed to project-system consumers. */
final class ProjectModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInTheProjectModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.project");
    }

    /** Keeps raw JSON and validation internals encapsulated. */
    @Test
    void exportsOnlySupportedProjectPackage() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.project.diagnostic",
                        "io.github.glynch.jscene3d.project.extension",
                        "io.github.glynch.jscene3d.project.manifest",
                        "io.github.glynch.jscene3d.project.scene",
                        "io.github.glynch.jscene3d.project.value");
    }
}
