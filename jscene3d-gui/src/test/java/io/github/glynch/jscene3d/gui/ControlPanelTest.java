/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

    /** Creates one primary-button press retained as held for the frame. */
    private static ControlPanel.PointerFrame pointer(double x, double y) {
        return new ControlPanel.PointerFrame(x, y, true, true, false);
    }
}
