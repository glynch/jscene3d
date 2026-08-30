/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class CoreModuleDescriptorTest {
    @Test
    void runsTestsInTheCoreModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.core");
    }
}
