/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.spatial3d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies the named-module boundary for the 3D project adapter. */
final class Spatial3dModuleDescriptorTest {
    /** Exports the stable adapter API package from the expected module. */
    @Test
    void exportsStableApi() {
        Module module = Spatial3dDescriptors.class.getModule();

        assertThat(module.getName()).isEqualTo("io.github.glynch.jscene3d.project.spatial3d");
        assertThat(module.isExported("io.github.glynch.jscene3d.project.spatial3d"))
                .isTrue();
    }
}
