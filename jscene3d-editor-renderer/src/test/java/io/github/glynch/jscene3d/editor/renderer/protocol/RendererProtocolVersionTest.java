/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.renderer.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/** Tests the explicit product renderer protocol version. */
final class RendererProtocolVersionTest {
    @Test
    void exposesCurrentWireVersion() {
        assertThat(RendererProtocolVersion.CURRENT)
                .isEqualTo(new RendererProtocolVersion(1, 2))
                .hasToString("1.2");
    }

    @Test
    void rejectsNegativeVersionParts() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RendererProtocolVersion(-1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new RendererProtocolVersion(1, -1));
    }
}
