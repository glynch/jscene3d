/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.inspector;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.selection.EditorSelection;
import io.github.glynch.jscene3d.editor.selection.EditorSelectionKinds;
import io.github.glynch.jscene3d.editor.view.EditorDetails;
import io.github.glynch.jscene3d.editor.view.EditorDetailsView;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class InspectorExtensionTest {
    @Test
    void contributesSelectionDrivenDetailsToTheSecondarySidebar() {
        EditorSelectionContext selections = new EditorSelectionContext();
        EditorExtensionHost host = new EditorExtensionHost(new EditorProjectContext(), selections);
        List<List<EditorViewContribution>> snapshots = new ArrayList<>();
        host.observeViews(snapshots::add);

        host.activate(new InspectorExtension());

        EditorViewContribution contribution = snapshots.getLast().getFirst();
        assertThat(contribution.container()).isEqualTo(EditorViewContainers.SECONDARY_SIDEBAR);
        assertThat(contribution.view().id()).isEqualTo(InspectorExtension.VIEW_ID);
        assertThat(contribution.view()).isInstanceOf(EditorDetailsView.class);
        EditorDetailsView view = (EditorDetailsView) contribution.view();
        assertThat(view.dataProvider().details()).isEmpty();

        List<Optional<EditorDetails>> observed = new ArrayList<>();
        view.dataProvider().observe(observed::add);
        EditorDetails details = new EditorDetails("Player", "Local entity", "world", "player", List.of(), List.of());
        selections.select(new EditorSelection(EditorSelectionKinds.LOCAL_ENTITY, "world:player", details));
        selections.clear();

        assertThat(observed).containsExactly(Optional.empty(), Optional.of(details), Optional.empty());
        host.close();
    }
}
