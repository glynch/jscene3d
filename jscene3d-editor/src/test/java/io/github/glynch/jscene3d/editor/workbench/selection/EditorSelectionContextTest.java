/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.selection;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class EditorSelectionContextTest {
    @Test
    void publishesOnlyGenuineSelectReplaceAndClearTransitions() {
        EditorSelectionContext selections = new EditorSelectionContext();
        List<Optional<EditorSelection>> observed = new ArrayList<>();
        EditorRegistration registration = selections.observe(observed::add);
        EditorSelection first = selection("first");
        EditorSelection second = selection("second");

        selections.select(first);
        selections.select(first);
        selections.select(second);
        selections.clear();
        selections.clear();
        registration.close();
        selections.select(first);

        assertThat(observed)
                .containsExactly(Optional.empty(), Optional.of(first), Optional.of(second), Optional.empty());
        assertThat(selections.current()).contains(first);
    }

    private static EditorSelection selection(String identity) {
        return new EditorSelection(EditorSelectionKinds.LOCAL_ENTITY, identity, Optional.empty());
    }
}
