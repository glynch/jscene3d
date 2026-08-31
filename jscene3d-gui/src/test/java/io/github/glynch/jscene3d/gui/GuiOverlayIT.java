/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.gui;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.platform.VerticalSync;
import io.github.glynch.jscene3d.platform.Window;
import io.github.glynch.jscene3d.platform.WindowOptions;
import io.github.glynch.jscene3d.render.Renderer;
import org.junit.jupiter.api.Test;

final class GuiOverlayIT {
    @Test
    void rendersControlPanelAndFpsMonitorThroughTheOwnedOverlayCanvas() {
        WindowOptions options = WindowOptions.builder()
                .size(320, 240)
                .title("GUI overlay integration test")
                .verticalSync(VerticalSync.DISABLED)
                .build();

        try (Window window = Window.create(options);
                Renderer renderer = Renderer.create(window)) {
            ControlPanel panel = new ControlPanel(window, "Controls");
            ControlPanel.Section section = panel.addSection("Display");
            section.addBoolean("enabled", () -> true, ignored -> {});
            section.addFloat("scale", () -> 0.5f, ignored -> {}, 0.0f, 1.0f);
            section.addButton("reset", () -> {});
            FpsMonitor monitor = new FpsMonitor();
            monitor.update(0L);
            monitor.update(16_666_667L);

            renderer.clear();
            renderer.render(panel);
            renderer.render(monitor);

            assertThat(renderer.info().resources().programCount()).isOne();
        }
    }
}
