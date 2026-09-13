/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.configuration.registry;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import org.junit.jupiter.api.Test;

final class ConfigurationModuleDescriptorTest {
    @Test
    void exportsTheDefinitionAndRegistryPackages() {
        ModuleDescriptor descriptor = getClass().getModule().getDescriptor();

        assertThat(descriptor.exports())
                .extracting(ModuleDescriptor.Exports::source)
                .containsExactlyInAnyOrder(
                        "io.github.glynch.jscene3d.configuration.definition",
                        "io.github.glynch.jscene3d.configuration.registry");
    }
}
