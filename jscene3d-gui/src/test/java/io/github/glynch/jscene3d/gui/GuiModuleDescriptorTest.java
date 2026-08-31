/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class GuiModuleDescriptorTest {
    @Test
    void runsTestsInTheGuiModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.gui");
    }

    @Test
    void exportsOnlyTheSupportedGuiPackage() {
        Set<String> exports = getClass().getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(exports).containsExactly("io.github.glynch.jscene3d.gui");
    }
}
