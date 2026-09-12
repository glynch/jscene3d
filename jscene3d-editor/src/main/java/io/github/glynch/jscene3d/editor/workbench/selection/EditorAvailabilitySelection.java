/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.selection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reconciles selection when context conditions change the ordered available items. */
public final class EditorAvailabilitySelection {
    private EditorAvailabilitySelection() {}

    /**
     * Selects a newly available leading item, otherwise retaining a still-available selection.
     *
     * @param previousAvailable previously available ordered items
     * @param previousSelected previous selection
     * @param available currently available ordered items
     * @param <T> item identity type
     * @return reconciled selection
     */
    public static <T> Optional<T> reconcile(
            List<T> previousAvailable, Optional<T> previousSelected, List<T> available) {
        List<T> previous = List.copyOf(Objects.requireNonNull(previousAvailable, "previousAvailable"));
        Optional<T> selected = Objects.requireNonNull(previousSelected, "previousSelected");
        List<T> current = List.copyOf(Objects.requireNonNull(available, "available"));
        if (current.isEmpty()) {
            return Optional.empty();
        }
        T leading = current.getFirst();
        if (!previous.contains(leading)) {
            return Optional.of(leading);
        }
        return selected.filter(current::contains).or(() -> Optional.of(leading));
    }
}
