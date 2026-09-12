/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.accessibility;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Orientation;
import javafx.scene.input.KeyCode;

/** Shared arrow, Home, and End navigation for compact workbench control groups. */
public final class JavaFxDirectionalNavigation {
    private JavaFxDirectionalNavigation() {}

    /**
     * Returns the item targeted by a directional navigation key.
     *
     * <p>Directional navigation wraps at either end. Home and End always select the first and last item.
     *
     * @param items ordered controls or identities
     * @param current currently focused item
     * @param key pressed key
     * @param orientation visual orientation of the group
     * @param <T> item type
     * @return navigation target, or empty when the key is not a group-navigation key
     */
    public static <T> Optional<T> target(List<T> items, T current, KeyCode key, Orientation orientation) {
        List<T> ordered = List.copyOf(Objects.requireNonNull(items, "items"));
        T selected = Objects.requireNonNull(current, "current");
        KeyCode pressed = Objects.requireNonNull(key, "key");
        Orientation direction = Objects.requireNonNull(orientation, "orientation");
        int currentIndex = ordered.indexOf(selected);
        if (ordered.isEmpty() || currentIndex < 0) {
            return Optional.empty();
        }
        if (pressed == KeyCode.HOME) {
            return Optional.of(ordered.getFirst());
        }
        if (pressed == KeyCode.END) {
            return Optional.of(ordered.getLast());
        }
        int step = step(pressed, direction);
        if (step == 0) {
            return Optional.empty();
        }
        return Optional.of(ordered.get(Math.floorMod(currentIndex + step, ordered.size())));
    }

    private static int step(KeyCode key, Orientation orientation) {
        if (orientation == Orientation.VERTICAL) {
            if (key == KeyCode.UP) {
                return -1;
            }
            if (key == KeyCode.DOWN) {
                return 1;
            }
            return 0;
        }
        if (key == KeyCode.LEFT) {
            return -1;
        }
        return key == KeyCode.RIGHT ? 1 : 0;
    }
}
