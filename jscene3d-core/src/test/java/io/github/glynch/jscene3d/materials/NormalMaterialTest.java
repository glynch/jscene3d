/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.materials;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import org.junit.jupiter.api.Test;

final class NormalMaterialTest {
    @Test
    void providesSharedMaterialDefaults() {
        try (NormalMaterial material = new NormalMaterial()) {
            assertThat(material.visible()).isTrue();
            assertThat(material.opacity()).isEqualTo(1.0f);
            assertThat(material.transparent()).isFalse();
            assertThat(material.side()).isEqualTo(MaterialSide.FRONT);
            assertThat(material.depthTestEnabled()).isTrue();
            assertThat(material.depthWriteEnabled()).isTrue();
            assertThat(material.version()).isZero();
        }
    }

    @Test
    void closesTerminally() {
        NormalMaterial material = new NormalMaterial();

        material.close();

        assertThat(material.isClosed()).isTrue();
        assertThatIllegalStateException().isThrownBy(material::visible);
    }
}
