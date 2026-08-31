/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.core.Color;
import org.junit.jupiter.api.Test;

final class OverlayGuiCanvasTest {
    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void rejectsNullBindingAndDrawingWhileUnbound() {
        OverlayGuiCanvas canvas = new OverlayGuiCanvas();

        assertThatNullPointerException().isThrownBy(() -> canvas.bind(null));
        assertThatIllegalStateException().isThrownBy(() -> canvas.rectangle(0.0f, 0.0f, 1.0f, 1.0f, Color.WHITE, 1.0f));
        assertThatIllegalStateException()
                .isThrownBy(() -> canvas.roundedRectangle(0.0f, 0.0f, 1.0f, 1.0f, 0.5f, Color.WHITE, 1.0f));
        assertThatIllegalStateException()
                .isThrownBy(() -> canvas.line(0.0f, 0.0f, 1.0f, 1.0f, 1.0f, Color.WHITE, 1.0f));
        assertThatIllegalStateException()
                .isThrownBy(() -> canvas.alphaMask(null, 0.0f, 0.0f, 1.0f, 1.0f, Color.WHITE, 1.0f));

        canvas.unbind();
    }
}
