/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gltf;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class GltfModuleDescriptorTest {
    /** Verifies that tests exercise the same named module delivered to users. */
    @Test
    void runsTestsInTheGltfModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.gltf");
    }

    /** Keeps parser and conversion implementation packages out of the public module interface. */
    @Test
    void exportsOnlyTheLoaderApi() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.gltf");
    }
}
