/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class LwjglModuleDescriptorTest {
    @Test
    void runsTestsInTheLwjglModule() {
        assertThat(getClass().getModule().getName()).isEqualTo("io.github.glynch.jscene3d.lwjgl");
    }
}
