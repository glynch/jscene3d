/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies stable selection transitions shared by editor browsing surfaces. */
final class EditorSelectionModelTest {
    /** Publishes only genuine select, replace, and clear transitions. */
    @Test
    void publishesStableTransitions() {
        EditorSelectionModel model = new EditorSelectionModel();
        List<Optional<EditorSelection>> observed = new ArrayList<>();
        Runnable unsubscribe = model.subscribe(observed::add);
        EditorSelection first = selection("first");
        EditorSelection second = selection("second");

        model.select(first);
        model.select(first);
        model.select(second);
        model.clear();
        model.clear();
        unsubscribe.run();
        model.select(first);

        assertThat(observed)
                .containsExactly(Optional.empty(), Optional.of(first), Optional.of(second), Optional.empty());
        assertThat(model.selection()).contains(first);
    }

    /** Creates a minimal immutable selection for transition testing. */
    private static EditorSelection selection(String identity) {
        EditorInspectorView inspector =
                new EditorInspectorView(identity, "Local entity", "worlds/test.world.json", identity, false, List.of());
        return new EditorSelection(EditorSelection.Kind.LOCAL_ENTITY, identity, inspector);
    }
}
