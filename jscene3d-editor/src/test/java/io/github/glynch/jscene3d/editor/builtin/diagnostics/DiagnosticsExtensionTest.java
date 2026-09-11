/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class DiagnosticsExtensionTest {
    @Test
    void contributesViewRevealCommandAndLiveStatusCounts() {
        EditorExtensionHost host = new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
        List<List<EditorViewContribution>> views = new ArrayList<>();
        List<List<EditorStatusItemSnapshot>> statuses = new ArrayList<>();
        List<ViewId> requests = new ArrayList<>();
        host.observeViews(views::add);
        host.observeStatusItems(statuses::add);
        host.observeViewRequests(requests::add);
        host.activate(new DiagnosticsExtension(host));

        assertThat(views.getLast()).singleElement().satisfies(contribution -> {
            assertThat(contribution.view().id()).isEqualTo(DiagnosticsView.VIEW_ID);
            assertThat(contribution.container()).isEqualTo(EditorViewContainers.BOTTOM_PANEL);
        });

        EditorDiagnosticCollection collection = hostCollection(host);
        collection.replaceAll(Map.of(
                URI.create("file:///project/example.java"),
                List.of(
                        diagnostic(EditorDiagnosticSeverity.ERROR, "broken"),
                        diagnostic(EditorDiagnosticSeverity.WARNING, "stale"))));

        assertThat(statuses.getLast()).extracting(item -> item.state().text()).containsExactly("1", "1");
        host.execute(EditorCommands.OPEN_DIAGNOSTICS);
        assertThat(requests).containsExactly(DiagnosticsView.VIEW_ID);
        host.close();
    }

    private static EditorDiagnosticCollection hostCollection(EditorExtensionHost host) {
        AtomicReference<EditorDiagnosticCollection> result = new AtomicReference<>();
        host.activate(new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.diagnostic-producer";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                result.set(context.subscriptions()
                        .add(context.diagnostics()
                                .createCollection(new DiagnosticCollectionId("io.github.glynch.test.diagnostics"))));
            }
        });
        return result.get();
    }

    private static EditorDiagnostic diagnostic(EditorDiagnosticSeverity severity, String message) {
        return new EditorDiagnostic(severity, "test.code", message, "", Optional.empty(), Map.of());
    }
}
