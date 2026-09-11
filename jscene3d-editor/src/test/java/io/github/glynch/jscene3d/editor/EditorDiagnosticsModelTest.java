/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

/** Verifies grouped diagnostic presentation independently of JavaFX controls. */
final class EditorDiagnosticsModelTest {
    private static final URI WORLD = URI.create("file:///project/worlds/map01.world.json");
    private static final URI IMPORT = URI.create("file:///project/imports/freedom.import.json");

    /** Groups by authoritative source while retaining complete and visible counts. */
    @Test
    void groupsDiagnosticsBySourceInArrivalOrder() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(List.of(
                diagnostic(ProjectDiagnostic.Severity.ERROR, WORLD, "/entities/0", "missing entity"),
                diagnostic(ProjectDiagnostic.Severity.WARNING, WORLD, "/entities/1", "disabled entity"),
                diagnostic(ProjectDiagnostic.Severity.ERROR, IMPORT, "", "missing source")));

        EditorDiagnosticsModel.View view = model.view();

        assertThat(view.errors()).isEqualTo(2L);
        assertThat(view.warnings()).isEqualTo(1L);
        assertThat(view.total()).isEqualTo(3L);
        assertThat(view.visible()).isEqualTo(3L);
        assertThat(view.groups())
                .extracting(EditorDiagnosticsModel.Group::label)
                .containsExactly("map01.world.json", "freedom.import.json");
        assertThat(view.groups().getFirst().items())
                .extracting(item -> item.diagnostic().location())
                .containsExactly("/entities/0", "/entities/1");
    }

    /** Searches messages, codes, sources, locations, and structured details case-insensitively. */
    @Test
    void filtersAcrossStructuredDiagnosticInformation() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(List.of(
                diagnostic(ProjectDiagnostic.Severity.ERROR, WORLD, "/entities/0", "missing entity"),
                diagnostic(ProjectDiagnostic.Severity.WARNING, IMPORT, "/source", "WAD fingerprint changed")));

        model.filter("FINGERPRINT");

        assertThat(model.view().visible()).isEqualTo(1L);
        assertThat(model.view().groups()).singleElement().returns(IMPORT, EditorDiagnosticsModel.Group::source);

        model.filter("/ENTITIES/0");

        assertThat(model.view().groups()).singleElement().returns(WORLD, EditorDiagnosticsModel.Group::source);
    }

    /** Severity filtering does not alter the complete counts displayed by the drawer and status bar. */
    @Test
    void filtersSeverityWithoutDiscardingCounts() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(List.of(
                diagnostic(ProjectDiagnostic.Severity.ERROR, WORLD, "", "broken"),
                diagnostic(ProjectDiagnostic.Severity.WARNING, IMPORT, "", "stale")));

        model.showSeverity(ProjectDiagnostic.Severity.ERROR, false);

        EditorDiagnosticsModel.View view = model.view();
        assertThat(view.errors()).isEqualTo(1L);
        assertThat(view.warnings()).isEqualTo(1L);
        assertThat(view.visible()).isEqualTo(1L);
        assertThat(view.groups()).singleElement().returns(IMPORT, EditorDiagnosticsModel.Group::source);
    }

    /** Keeps the fallback meaning and exact source available below the concise occurrence detail. */
    @Test
    void projectsExpandableDetails() {
        EditorDiagnosticsModel model = new EditorDiagnosticsModel();
        model.showDiagnostics(
                List.of(diagnostic(ProjectDiagnostic.Severity.ERROR, WORLD, "/entities/0", "missing entity")));

        EditorDiagnosticsModel.Item item =
                model.view().groups().getFirst().items().getFirst();

        assertThat(item.summary()).isEqualTo("missing entity");
        assertThat(item.details())
                .extracting(EditorDiagnosticsModel.Detail::label, EditorDiagnosticsModel.Detail::value)
                .containsExactly(
                        Tuple.tuple("Meaning", "Test diagnostic"),
                        Tuple.tuple("Source", WORLD.toString()),
                        Tuple.tuple("Location", "/entities/0"));
    }

    /** Creates one representative structured project diagnostic. */
    private static ProjectDiagnostic diagnostic(
            ProjectDiagnostic.Severity severity, URI source, String location, String detail) {
        return new ProjectDiagnostic(severity, TestCode.TEST, source, location, Map.of("technicalDetail", detail));
    }

    /** Stable test code with a distinct fallback message. */
    private enum TestCode implements DiagnosticCode {
        TEST;

        @Override
        public String code() {
            return "test.code";
        }

        @Override
        public String defaultMessage() {
            return "Test diagnostic";
        }
    }
}
