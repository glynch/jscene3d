/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.diagnostic.DiagnosticCode;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ProjectDiagnosticsExtensionTest {
    @Test
    void publishesProjectDiagnosticsThroughTheSharedEditorCollection() {
        EditorExtensionHost host = new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
        List<List<EditorDiagnosticSnapshot>> snapshots = new ArrayList<>();
        host.observeDiagnostics(snapshots::add);
        ProjectDiagnosticsExtension extension = new ProjectDiagnosticsExtension();
        host.activate(extension);

        URI source = URI.create("file:///project/jscene3d.json");
        extension.showDiagnostics(List.of(new ProjectDiagnostic(
                ProjectDiagnostic.Severity.ERROR,
                TestCode.INVALID,
                source,
                "/worlds/0",
                Map.of("technicalDetail", "World does not exist", "world", "MAP01"))));

        assertThat(snapshots.getLast()).singleElement().satisfies(snapshot -> {
            assertThat(snapshot.source()).isEqualTo(source);
            assertThat(snapshot.diagnostic().message()).isEqualTo("World does not exist");
            assertThat(snapshot.diagnostic().details()).containsExactlyEntriesOf(Map.of("world", "MAP01"));
        });
        host.close();
    }

    private enum TestCode implements DiagnosticCode {
        INVALID;

        @Override
        public String code() {
            return "test.invalid";
        }

        @Override
        public String defaultMessage() {
            return "Invalid project";
        }
    }
}
