/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class LwjglModuleDescriptorTest {
    @Test
    void runsTestsInTheLwjglModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.lwjgl");
    }

    @Test
    void exportsOnlySupportedFeaturePackages() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.controls",
                        "io.github.glynch.jscene3d.loaders",
                        "io.github.glynch.jscene3d.platform",
                        "io.github.glynch.jscene3d.render");
    }
}
