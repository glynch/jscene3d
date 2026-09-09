/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.desktop;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.game.input.InputCapture;
import org.junit.jupiter.api.Test;

/** Verifies desktop pointer ownership independently of a native window. */
final class DesktopPointerCaptureTest {
    private static final InputCapture POINTER_CAPTURE = new InputCapture(false, true);

    @Test
    void suppressesReleasedPointerThenDeliversTheCaptureClickToTheProject() {
        DesktopPointerCapture pointer = new DesktopPointerCapture(true);

        assertThat(pointer.update(true, false, false))
                .isEqualTo(new DesktopPointerCapture.Update(POINTER_CAPTURE, false, false));
        assertThat(pointer.update(true, false, true))
                .isEqualTo(new DesktopPointerCapture.Update(InputCapture.NONE, true, false));
        assertThat(pointer.update(true, false, false))
                .isEqualTo(new DesktopPointerCapture.Update(InputCapture.NONE, false, false));
        assertThat(pointer.update(true, false, true))
                .isEqualTo(new DesktopPointerCapture.Update(InputCapture.NONE, false, false));
    }

    @Test
    void escapeAndFocusLossReleaseWithoutRequestingApplicationClosure() {
        DesktopPointerCapture pointer = new DesktopPointerCapture(true);
        pointer.update(true, false, true);

        assertThat(pointer.update(true, true, false))
                .isEqualTo(new DesktopPointerCapture.Update(POINTER_CAPTURE, false, true));
        assertThat(pointer.update(true, true, false))
                .isEqualTo(new DesktopPointerCapture.Update(POINTER_CAPTURE, false, false));

        pointer.update(true, false, true);
        assertThat(pointer.update(false, false, false))
                .isEqualTo(new DesktopPointerCapture.Update(POINTER_CAPTURE, false, true));
    }

    @Test
    void leavesOrdinaryPointerProjectsUncaptured() {
        DesktopPointerCapture pointer = new DesktopPointerCapture(false);

        assertThat(pointer.update(true, false, true))
                .isEqualTo(new DesktopPointerCapture.Update(InputCapture.NONE, false, false));
        assertThat(pointer.update(false, true, false))
                .isEqualTo(new DesktopPointerCapture.Update(InputCapture.NONE, false, false));
    }
}
