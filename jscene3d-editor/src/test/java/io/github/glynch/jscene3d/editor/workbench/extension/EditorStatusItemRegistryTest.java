/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EditorStatusItemRegistryTest {
    @Test
    void ordersItemsByRegionAndDescendingPriority() {
        EditorStatusItemRegistry registry = new EditorStatusItemRegistry();
        List<List<EditorStatusItemSnapshot>> snapshots = new ArrayList<>();
        registry.observe(snapshots::add);
        register(registry, "right-low", StatusBarAlignment.RIGHT, 10);
        register(registry, "left-low", StatusBarAlignment.LEFT, 10);
        register(registry, "right-high", StatusBarAlignment.RIGHT, 20);
        register(registry, "left-high", StatusBarAlignment.LEFT, 20);

        assertThat(snapshots.getLast())
                .extracting(item -> item.contribution().id().value())
                .containsExactly(
                        "io.github.glynch.test.left-high",
                        "io.github.glynch.test.left-low",
                        "io.github.glynch.test.right-high",
                        "io.github.glynch.test.right-low");
        registry.close();
    }

    private static void register(
            EditorStatusItemRegistry registry, String name, StatusBarAlignment alignment, int priority) {
        registry.create(new EditorStatusItemContribution(
                new StatusItemId("io.github.glynch.test." + name), alignment, priority));
    }
}
