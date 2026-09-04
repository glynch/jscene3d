/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class CoreModuleDescriptorTest {
    @Test
    void runsTestsInTheCoreModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.core");
    }

    @Test
    void exportsOnlySupportedFeaturePackages() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.animation",
                        "io.github.glynch.jscene3d.cameras",
                        "io.github.glynch.jscene3d.diagnostic",
                        "io.github.glynch.jscene3d.fogs",
                        "io.github.glynch.jscene3d.geometries",
                        "io.github.glynch.jscene3d.helpers",
                        "io.github.glynch.jscene3d.io",
                        "io.github.glynch.jscene3d.lights",
                        "io.github.glynch.jscene3d.materials",
                        "io.github.glynch.jscene3d.math",
                        "io.github.glynch.jscene3d.objects",
                        "io.github.glynch.jscene3d.raycasting",
                        "io.github.glynch.jscene3d.scenes",
                        "io.github.glynch.jscene3d.textures");
    }
}
