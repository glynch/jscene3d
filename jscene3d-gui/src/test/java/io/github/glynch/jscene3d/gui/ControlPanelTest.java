/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.gui.internal.GuiCanvas;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class ControlPanelTest {
    @Test
    void appliesExplicitBooleanSliderAndButtonBindings() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");
        AtomicBoolean enabled = new AtomicBoolean(true);
        float[] speed = {0.0f};
        AtomicInteger resetCount = new AtomicInteger();
        section.addBoolean("enabled", enabled::get, enabled::set);
        section.addFloat("speed", () -> speed[0], value -> speed[0] = value, 0.0f, 10.0f);
        section.addButton("reset", resetCount::incrementAndGet);

        assertThat(panel.update(pointer(520.0, 100.0), 800, 600)).isTrue();
        assertThat(enabled).isFalse();
        assertThat(panel.capturesPointer()).isTrue();
        panel.update(new ControlPanel.PointerFrame(100.0, 100.0, false, true, false), 800, 600);
        assertThat(panel.capturesPointer()).isTrue();
        panel.update(new ControlPanel.PointerFrame(100.0, 100.0, false, false, true), 800, 600);

        assertThat(panel.update(pointer(685.0, 140.0), 800, 600)).isTrue();
        assertThat(speed[0]).isEqualTo(5.0f);

        assertThat(panel.update(pointer(520.0, 180.0), 800, 600)).isTrue();
        assertThat(resetCount).hasValue(1);
    }

    @Test
    void collapsesSectionsAndReleasesPointerOutsideThePanel() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");
        section.addBoolean("enabled", () -> true, ignored -> {});

        assertThat(panel.update(pointer(520.0, 70.0), 800, 600)).isTrue();
        assertThat(section.isExpanded()).isFalse();

        panel.update(new ControlPanel.PointerFrame(100.0, 100.0, false, false, true), 800, 600);
        panel.update(new ControlPanel.PointerFrame(100.0, 100.0, false, false, false), 800, 600);
        assertThat(panel.capturesPointer()).isFalse();
    }

    @Test
    void rejectsInvalidLabelsAndSliderIntervals() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");

        assertThatIllegalArgumentException().isThrownBy(() -> panel.addSection(" "));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> section.addFloat("speed", () -> 1.0f, ignored -> {}, 2.0f, 1.0f));
    }

    @Test
    void paintsExpandedControlsAndTheirCurrentStates() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");
        section.addBoolean("enabled", () -> true, ignored -> {});
        section.addFloat("speed", () -> 5.0f, ignored -> {}, 0.0f, 10.0f);
        section.addButton("reset", () -> {});
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        panel.paint(canvas, 800, 600);

        assertThat(canvas.rectangleCount()).isEqualTo(7);
        assertThat(canvas.roundedRectangleCount()).isEqualTo(10);
        assertThat(canvas.lineCount()).isEqualTo(4);
        assertThat(canvas.alphaMaskCount()).isPositive();
    }

    @Test
    void paintsCollapsedAndHoveredSectionsWithoutTheirControls() {
        GuiTheme theme = GuiTheme.dark();
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");
        section.addBoolean("enabled", () -> false, ignored -> {});
        section.setExpanded(false);
        panel.update(new ControlPanel.PointerFrame(520.0, 70.0, false, false, false), 800, 600);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        panel.paint(canvas, 800, 600);

        assertThat(canvas.rectangleCount()).isOne();
        assertThat(canvas.rectangleColors()).containsExactly(theme.rowHover());
        assertThat(canvas.roundedRectangleCount()).isEqualTo(4);
        assertThat(canvas.lineCount()).isEqualTo(2);
    }

    @Test
    void paintsUncheckedAndDegenerateSliderBranches() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");
        section.addBoolean("enabled", () -> false, ignored -> {});
        section.addFloat("fixed", () -> 1.0f, ignored -> {}, 1.0f, 1.0f);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        panel.paint(canvas, 800, 600);

        assertThat(canvas.lineCount()).isEqualTo(2);
        assertThat(canvas.roundedRectangleCount()).isEqualTo(9);
    }

    @Test
    void hiddenPanelNeitherPaintsNorCapturesInput() {
        ControlPanel panel = new ControlPanel("Controls");
        panel.addSection("Camera").addFloat("speed", () -> 1.0f, ignored -> {}, 0.0f, 2.0f);
        panel.update(pointer(685.0, 100.0), 800, 600);
        panel.setVisible(false);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        assertThat(panel.update(pointer(685.0, 100.0), 800, 600)).isFalse();
        panel.paint(canvas, 800, 600);

        assertThat(panel.isVisible()).isFalse();
        assertThat(panel.capturesPointer()).isFalse();
        assertThat(canvas.commandCount()).isZero();
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void validatesPaintAndDetachedWindowAccess() {
        ControlPanel panel = new ControlPanel("Controls");
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();

        assertThatNullPointerException().isThrownBy(() -> panel.paint((GuiCanvas) null, 800, 600));
        assertThatIllegalArgumentException().isThrownBy(() -> panel.paint(canvas, 0, 600));
        assertThatIllegalArgumentException().isThrownBy(() -> panel.paint(canvas, 800, 0));
        assertThatIllegalStateException().isThrownBy(panel::update);
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberately exercises runtime null validation.
    void validatesControlBindingsAndSliderValues() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Camera");

        assertThatNullPointerException().isThrownBy(() -> section.addBoolean("enabled", null, ignored -> {}));
        assertThatNullPointerException().isThrownBy(() -> section.addBoolean("enabled", () -> true, null));
        assertThatNullPointerException().isThrownBy(() -> section.addFloat("speed", null, ignored -> {}, 0.0f, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> section.addFloat("speed", () -> 0.0f, null, 0.0f, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> section.addButton("reset", null));
        section.addFloat("invalid", () -> Float.NaN, ignored -> {}, 0.0f, 1.0f);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();
        assertThatIllegalArgumentException().isThrownBy(() -> panel.paint(canvas, 800, 600));
    }

    /** Creates one primary-button press retained as held for the frame. */
    private static ControlPanel.PointerFrame pointer(double x, double y) {
        return new ControlPanel.PointerFrame(x, y, true, true, false);
    }
}
