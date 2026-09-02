/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.physics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class PhysicsModuleDescriptorTest {
    @Test
    void runsTestsInThePhysicsModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.physics");
    }

    @Test
    void exportsOnlyTheSupportedPhysicsApi() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.physics",
                        "io.github.glynch.jscene3d.physics.debug",
                        "io.github.glynch.jscene3d.physics.movement",
                        "io.github.glynch.jscene3d.physics.queries",
                        "io.github.glynch.jscene3d.physics.shapes");
    }
}
