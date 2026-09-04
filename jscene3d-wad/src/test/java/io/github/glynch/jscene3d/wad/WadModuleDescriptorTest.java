/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.wad;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the named-module interface exposed to WAD consumers. */
final class WadModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInTheWadModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.wad");
    }

    /** Keeps binary-decoding implementation packages encapsulated. */
    @Test
    void exportsOnlySupportedWadPackages() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.wad");
    }
}
