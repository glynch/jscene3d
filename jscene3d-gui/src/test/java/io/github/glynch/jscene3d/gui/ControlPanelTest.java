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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class ControlPanelTest {
    @Test
    void paintsFloatSlidersAtTheirConfiguredPrecision() {
        ControlPanel roundedPanel = new ControlPanel("Controls");
        roundedPanel.addSection("Orientation").addFloat("sensitivity", () -> 0.00525f, ignored -> {}, 0.0005f, 0.01f);
        ControlPanel precisePanel = new ControlPanel("Controls");
        precisePanel
                .addSection("Orientation")
                .addFloat("sensitivity", () -> 0.00525f, ignored -> {}, 0.0005f, 0.01f, 4);
        RecordingGuiCanvas roundedCanvas = new RecordingGuiCanvas();
        RecordingGuiCanvas preciseCanvas = new RecordingGuiCanvas();

        roundedPanel.paint(roundedCanvas, 800, 600);
        precisePanel.paint(preciseCanvas, 800, 600);

        assertThat(preciseCanvas.alphaMaskCount()).isEqualTo(roundedCanvas.alphaMaskCount() + 2);
    }

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
    void preventsDisabledButtonActivationUntilItsBindingEnablesIt() {
        ControlPanel panel = new ControlPanel("Controls");
        AtomicBoolean enabled = new AtomicBoolean();
        AtomicInteger invocationCount = new AtomicInteger();
        panel.addSection("Actions").addButton("apply", enabled::get, invocationCount::incrementAndGet);

        assertThat(panel.update(pointer(520.0, 100.0), 800, 600)).isFalse();
        assertThat(invocationCount).hasValue(0);

        enabled.set(true);
        assertThat(panel.update(pointer(520.0, 100.0), 800, 600)).isTrue();
        assertThat(invocationCount).hasValue(1);
    }

    @Test
    void preventsInteractionWithConditionallyDisabledSections() {
        ControlPanel panel = new ControlPanel("Controls");
        AtomicBoolean enabled = new AtomicBoolean();
        float[] density = {0.025f};
        ControlPanel.Section section = panel.addSection("Exponential squared");
        section.setEnabled(enabled::get);
        section.addFloat("density", () -> density[0], value -> density[0] = value, 0.0f, 0.08f);

        assertThat(panel.update(pointer(685.0, 100.0), 800, 600)).isFalse();
        assertThat(density[0]).isEqualTo(0.025f);

        RecordingGuiCanvas disabledCanvas = new RecordingGuiCanvas();
        panel.paint(disabledCanvas, 800, 600);
        enabled.set(true);
        RecordingGuiCanvas enabledCanvas = new RecordingGuiCanvas();
        panel.paint(enabledCanvas, 800, 600);

        assertThat(disabledCanvas.rectangleCount()).isEqualTo(enabledCanvas.rectangleCount() + 1);
        assertThat(panel.update(pointer(685.0, 100.0), 800, 600)).isTrue();
        assertThat(density[0]).isEqualTo(0.04f);
    }

    @Test
    void appliesExplicitIntegerAndChoiceBindings() {
        ControlPanel panel = new ControlPanel("Controls");
        ControlPanel.Section section = panel.addSection("Geometry");
        AtomicInteger segments = new AtomicInteger(2);
        AtomicReference<String> shading = new AtomicReference<>("smooth");
        section.addInteger("segments", segments::get, segments::set, 2, 50);
        section.addChoice(
                "shading",
                shading::get,
                shading::set,
                List.of(
                        new ControlPanel.Choice<>("wireframe", "wireframe"),
                        new ControlPanel.Choice<>("smooth", "smooth"),
                        new ControlPanel.Choice<>("reflective", "reflective")));

        assertThat(panel.update(pointer(730.0, 100.0), 800, 600)).isTrue();
        assertThat(segments).hasValue(50);
        panel.update(new ControlPanel.PointerFrame(730.0, 100.0, false, false, true), 800, 600);

        assertThat(panel.update(pointer(700.0, 140.0), 800, 600)).isTrue();
        assertThat(shading).hasValue("reflective");
        panel.update(new ControlPanel.PointerFrame(700.0, 140.0, false, false, true), 800, 600);

        RecordingGuiCanvas canvas = new RecordingGuiCanvas();
        panel.paint(canvas, 800, 600);
        assertThat(canvas.roundedRectangleCount()).isGreaterThan(4);
        assertThat(canvas.lineCount()).isGreaterThan(4);

        assertThat(panel.update(pointer(700.0, 140.0), 800, 600)).isFalse();
        panel.update(new ControlPanel.PointerFrame(700.0, 140.0, false, false, true), 800, 600);

        assertThat(panel.update(pointer(520.0, 140.0), 800, 600)).isTrue();
        assertThat(shading).hasValue("smooth");
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
        assertThatIllegalArgumentException()
                .isThrownBy(() -> section.addFloat("speed", () -> 1.0f, ignored -> {}, 0.0f, 1.0f, 0));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> section.addFloat("speed", () -> 1.0f, ignored -> {}, 0.0f, 1.0f, 5));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> section.addInteger("segments", () -> 2, ignored -> {}, 50, 2));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> section.addChoice("shading", () -> "smooth", ignored -> {}, List.of()));
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
    void paintsCurrentReadOnlyTextWithoutActivatingIt() {
        ControlPanel panel = new ControlPanel("Controls");
        AtomicReference<String> selected = new AtomicReference<>("none");
        panel.addSection("Selection").addText("selected", selected::get);
        RecordingGuiCanvas firstCanvas = new RecordingGuiCanvas();

        panel.paint(firstCanvas, 800, 600);
        selected.set("cyan box");
        RecordingGuiCanvas secondCanvas = new RecordingGuiCanvas();
        panel.paint(secondCanvas, 800, 600);

        assertThat(firstCanvas.alphaMaskCount()).isPositive();
        assertThat(secondCanvas.alphaMaskCount()).isGreaterThan(firstCanvas.alphaMaskCount());
        assertThat(panel.update(pointer(520.0, 100.0), 800, 600)).isFalse();
        assertThat(panel.capturesPointer()).isTrue();
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

        assertThatNullPointerException().isThrownBy(() -> section.setEnabled(null));
        assertThatNullPointerException().isThrownBy(() -> section.addBoolean("enabled", null, ignored -> {}));
        assertThatNullPointerException().isThrownBy(() -> section.addBoolean("enabled", () -> true, null));
        assertThatNullPointerException().isThrownBy(() -> section.addFloat("speed", null, ignored -> {}, 0.0f, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> section.addFloat("speed", () -> 0.0f, null, 0.0f, 1.0f));
        assertThatNullPointerException().isThrownBy(() -> section.addButton("reset", null));
        assertThatNullPointerException().isThrownBy(() -> section.addButton("reset", null, () -> {}));
        assertThatNullPointerException().isThrownBy(() -> section.addButton("reset", () -> true, null));
        assertThatNullPointerException().isThrownBy(() -> section.addText("selected", null));
        assertThatNullPointerException().isThrownBy(() -> section.addInteger("segments", null, ignored -> {}, 2, 50));
        assertThatNullPointerException()
                .isThrownBy(() -> section.addChoice(
                        "shading", null, ignored -> {}, List.of(new ControlPanel.Choice<>("smooth", "smooth"))));
        section.addFloat("invalid", () -> Float.NaN, ignored -> {}, 0.0f, 1.0f);
        RecordingGuiCanvas canvas = new RecordingGuiCanvas();
        assertThatIllegalArgumentException().isThrownBy(() -> panel.paint(canvas, 800, 600));
    }

    @Test
    void rejectsNullTextValuesWhenPainted() {
        ControlPanel panel = new ControlPanel("Controls");
        panel.addSection("Selection").addText("selected", () -> null);

        assertThatNullPointerException().isThrownBy(() -> panel.paint(new RecordingGuiCanvas(), 800, 600));
    }

    /** Creates one primary-button press retained as held for the frame. */
    private static ControlPanel.PointerFrame pointer(double x, double y) {
        return new ControlPanel.PointerFrame(x, y, true, true, false);
    }
}
