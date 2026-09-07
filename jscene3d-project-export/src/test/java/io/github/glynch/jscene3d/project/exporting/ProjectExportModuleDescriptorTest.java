/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.exporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the project-export JPMS surface from within the named test module. */
final class ProjectExportModuleDescriptorTest {
    /** Confirms tests execute in the production named module. */
    @Test
    void runsTestsInTheProjectExportModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.project.exporting");
    }

    /** Keeps implementation and command adaptation inaccessible to module-path callers. */
    @Test
    void exportsOnlyTheApplicationImageInterface() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();
        Set<String> exports = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.project.exporting");
    }
}
