/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.audio;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Verifies the named-module boundary exposed to audio consumers. */
final class AudioModuleDescriptorTest {
    /** Confirms tests execute inside the production named module. */
    @Test
    void runsTestsInTheAudioModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.audio");
    }

    /** Keeps OpenAL and decoder implementation packages encapsulated. */
    @Test
    void exportsOnlySupportedAudioPackages() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.audio");
    }
}
