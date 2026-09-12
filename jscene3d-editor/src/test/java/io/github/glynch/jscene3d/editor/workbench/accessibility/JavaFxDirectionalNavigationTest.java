/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;

/** Exercises shared keyboard navigation for workbench control groups. */
final class JavaFxDirectionalNavigationTest {
    private static final List<String> ITEMS = List.of("first", "middle", "last");

    @Test
    void navigatesAndWrapsAVerticalActivityGroup() {
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "middle", KeyCode.UP, Orientation.VERTICAL))
                .contains("first");
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "last", KeyCode.DOWN, Orientation.VERTICAL))
                .contains("first");
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "middle", KeyCode.RIGHT, Orientation.VERTICAL))
                .isEmpty();
    }

    @Test
    void navigatesAndWrapsAHorizontalTabGroup() {
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "middle", KeyCode.RIGHT, Orientation.HORIZONTAL))
                .contains("last");
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "first", KeyCode.LEFT, Orientation.HORIZONTAL))
                .contains("last");
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "middle", KeyCode.DOWN, Orientation.HORIZONTAL))
                .isEmpty();
    }

    @Test
    void supportsHomeAndEndInEitherOrientation() {
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "middle", KeyCode.HOME, Orientation.VERTICAL))
                .contains("first");
        assertThat(JavaFxDirectionalNavigation.target(ITEMS, "middle", KeyCode.END, Orientation.HORIZONTAL))
                .contains("last");
    }
}
