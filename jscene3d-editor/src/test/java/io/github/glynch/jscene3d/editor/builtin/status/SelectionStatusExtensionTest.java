/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.status;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies contextual status publication independently of JavaFX rendering. */
final class SelectionStatusExtensionTest {
    @Test
    void followsInspectableReadOnlySelectionState() {
        EditorSelectionContext selections = new EditorSelectionContext();
        EditorExtensionHost host = new EditorExtensionHost(new EditorProjectContext(), selections);
        List<List<EditorStatusItemSnapshot>> snapshots = new ArrayList<>();
        host.observeStatusItems(snapshots::add);
        host.activate(new SelectionStatusExtension());

        assertThat(state(snapshots).visible()).isFalse();

        EditorIcon readOnly = new EditorIcon(EditorIcons.READ_ONLY, "Generated content · read-only");
        EditorDetails generated = new EditorDetails(
                "Door 27", "Generated entity", "worlds/map01.world.json", "door-27", List.of(readOnly), List.of());
        selections.select(new EditorSelection(EditorSelectionKinds.GENERATED_ENTITY, "world:door-27", generated));

        EditorStatusItemState selected = state(snapshots);
        assertThat(selected.text()).isEqualTo("Generated entity · Read-only");
        assertThat(selected.icon()).contains(readOnly);
        assertThat(selected.tooltip()).contains("Door 27 · Generated content · read-only");
        assertThat(selected.visible()).isTrue();

        EditorDetails writable =
                new EditorDetails("Draft", "Local entity", "worlds/draft.world.json", "draft", List.of(), List.of());
        selections.select(new EditorSelection(EditorSelectionKinds.LOCAL_ENTITY, "world:draft", writable));

        assertThat(state(snapshots).visible()).isFalse();
        host.close();
    }

    private static EditorStatusItemState state(List<List<EditorStatusItemSnapshot>> snapshots) {
        return snapshots.getLast().stream()
                .filter(item -> item.contribution().id().equals(SelectionStatusExtension.STATUS_ID))
                .findFirst()
                .orElseThrow()
                .state();
    }
}
