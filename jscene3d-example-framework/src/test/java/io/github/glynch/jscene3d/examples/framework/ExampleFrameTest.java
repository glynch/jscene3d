/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.examples.framework;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Verifies that hosted examples can distinguish pointer and keyboard ownership. */
final class ExampleFrameTest {
    /** Pointer capture must not suppress keyboard-driven movement. */
    @Test
    void representsPointerAndKeyboardCaptureIndependently() {
        ExampleFrame frame = new ExampleFrame(1.0F / 60.0F, true, false);

        assertThat(frame.pointerCaptured()).isTrue();
        assertThat(frame.keyboardCaptured()).isFalse();
    }
}
