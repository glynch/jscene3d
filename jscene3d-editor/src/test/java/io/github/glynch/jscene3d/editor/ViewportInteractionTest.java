/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Tests deterministic transfer of JavaFX input into rendered frames. */
final class ViewportInteractionTest {

    @Test
    void exposesPreviewDefaults() {
        ViewportInteraction interaction = new ViewportInteraction();

        assertThat(interaction.consume())
                .returns(true, ViewportInteraction.FrameInput::spinning)
                .returns(0.7f, ViewportInteraction.FrameInput::rotationSpeed)
                .returns(0.0f, ViewportInteraction.FrameInput::horizontalDrag)
                .returns(0.0f, ViewportInteraction.FrameInput::verticalDrag)
                .returns(false, ViewportInteraction.FrameInput::resetRequested);
    }

    @Test
    void consumesTransientInputOnceWithoutDiscardingPersistentSettings() {
        ViewportInteraction interaction = new ViewportInteraction();
        interaction.setSpinning(false);
        interaction.setRotationSpeed(1.25f);
        interaction.drag(4.0f, -2.5f);
        interaction.drag(3.0f, 1.0f);
        interaction.requestReset();

        assertThat(interaction.consume())
                .returns(false, ViewportInteraction.FrameInput::spinning)
                .returns(1.25f, ViewportInteraction.FrameInput::rotationSpeed)
                .returns(7.0f, ViewportInteraction.FrameInput::horizontalDrag)
                .returns(-1.5f, ViewportInteraction.FrameInput::verticalDrag)
                .returns(true, ViewportInteraction.FrameInput::resetRequested);
        assertThat(interaction.consume())
                .returns(false, ViewportInteraction.FrameInput::spinning)
                .returns(1.25f, ViewportInteraction.FrameInput::rotationSpeed)
                .returns(0.0f, ViewportInteraction.FrameInput::horizontalDrag)
                .returns(0.0f, ViewportInteraction.FrameInput::verticalDrag)
                .returns(false, ViewportInteraction.FrameInput::resetRequested);
    }
}
