/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies the reusable presentation of the approved JScene3D product mark. */
final class EditorBrandMarkTest {
    /** Resolves the packaged derivative independently of the JavaFX graphics lifecycle. */
    @Test
    void resolvesPackagedMark() {
        assertThat(EditorBrandMark.resource()).isNotNull();
    }
}
