/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

/** Verifies grouped diagnostic presentation independently of JavaFX controls. */
final class EditorDiagnosticsModelTest {
    private static final DiagnosticCollectionId COLLECTION =
            new DiagnosticCollectionId("io.github.glynch.test.diagnostics");
    private static final URI WORLD = URI.create("file:///project/worlds/map01.world.json");
    private static final URI IMPORT = URI.create("file:///project/imports/freedom.import.json");

    @Test
    void groupsDiagnosticsBySourceAndRetainsAllSeverityCounts() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(List.of(
                diagnostic(EditorDiagnosticSeverity.ERROR, WORLD, "/entities/0", "missing entity"),
                diagnostic(EditorDiagnosticSeverity.WARNING, WORLD, "/entities/1", "disabled entity"),
                diagnostic(EditorDiagnosticSeverity.INFORMATION, IMPORT, "", "indexed source"),
                diagnostic(EditorDiagnosticSeverity.HINT, IMPORT, "", "consider naming")));

        EditorDiagnosticsModel.View view = model.view();

        assertThat(view.errors()).isEqualTo(1L);
        assertThat(view.warnings()).isEqualTo(1L);
        assertThat(view.information()).isEqualTo(1L);
        assertThat(view.hints()).isEqualTo(1L);
        assertThat(view.total()).isEqualTo(4L);
        assertThat(view.groups())
                .extracting(EditorDiagnosticsModel.Group::label)
                .containsExactly("map01.world.json", "freedom.import.json");
    }

    @Test
    void filtersAcrossStructuredDiagnosticInformation() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(List.of(
                diagnostic(EditorDiagnosticSeverity.ERROR, WORLD, "/entities/0", "missing entity"),
                diagnostic(EditorDiagnosticSeverity.WARNING, IMPORT, "/source", "WAD fingerprint changed")));

        model.filter("FINGERPRINT");

        assertThat(model.view().groups()).singleElement().returns(IMPORT, EditorDiagnosticsModel.Group::source);

        model.filter("/ENTITIES/0");

        assertThat(model.view().groups()).singleElement().returns(WORLD, EditorDiagnosticsModel.Group::source);
    }

    @Test
    void filtersSeverityWithoutDiscardingCompleteCounts() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(List.of(
                diagnostic(EditorDiagnosticSeverity.ERROR, WORLD, "", "broken"),
                diagnostic(EditorDiagnosticSeverity.WARNING, IMPORT, "", "stale")));

        model.showSeverity(EditorDiagnosticSeverity.ERROR, false);

        EditorDiagnosticsModel.View view = model.view();
        assertThat(view.errors()).isEqualTo(1L);
        assertThat(view.warnings()).isEqualTo(1L);
        assertThat(view.visible()).isEqualTo(1L);
        assertThat(view.groups()).singleElement().returns(IMPORT, EditorDiagnosticsModel.Group::source);
    }

    @Test
    void projectsDetailsAndPortableCopyText() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        EditorDiagnostic diagnostic = new EditorDiagnostic(
                EditorDiagnosticSeverity.ERROR,
                "test.code",
                "missing entity",
                "/entities/0",
                Optional.empty(),
                Map.of("expectedType", "entity"));
        model.showDiagnostics(List.of(new EditorDiagnosticSnapshot(COLLECTION, WORLD, diagnostic)));

        EditorDiagnosticsModel.Item item =
                model.view().groups().getFirst().items().getFirst();

        assertThat(item.details())
                .extracting(EditorDiagnosticsModel.Detail::label, EditorDiagnosticsModel.Detail::value)
                .containsExactly(Tuple.tuple("expectedType", "entity"));
        assertThat(item.copyText())
                .isEqualTo("missing entity [test.code] file:///project/worlds/map01.world.json /entities/0");
    }

    private static EditorDiagnosticSnapshot diagnostic(
            EditorDiagnosticSeverity severity, URI source, String location, String message) {
        return new EditorDiagnosticSnapshot(
                COLLECTION,
                source,
                new EditorDiagnostic(severity, "test.code", message, location, Optional.empty(), Map.of()));
    }
}
