/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.text;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorDiagnosticSnapshot;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class MonacoDiagnosticsJsonTest {
    @Test
    void encodesRangesAsOneBasedMonacoMarkersAndEscapesMessages() {
        EditorDiagnostic diagnostic = new EditorDiagnostic(
                EditorDiagnosticSeverity.ERROR,
                "java-1",
                "Java",
                "Expected \";\"\nnext line",
                "2:4",
                Optional.of(new EditorTextRange(new EditorTextPosition(1, 3), new EditorTextPosition(1, 6))),
                Map.of());
        EditorDiagnosticSnapshot snapshot = new EditorDiagnosticSnapshot(
                new DiagnosticCollectionId("io.github.glynch.test.java"),
                URI.create("file:///Example.java"),
                diagnostic);

        assertThat(MonacoDiagnosticsJson.encode(List.of(snapshot)))
                .isEqualTo("[{\"startLineNumber\":2,\"startColumn\":4,\"endLineNumber\":2,"
                        + "\"endColumn\":7,\"severity\":\"ERROR\",\"message\":\"Expected \\\";\\\"\\nnext line\","
                        + "\"code\":\"java-1\",\"source\":\"Java\"}]");
    }

    @Test
    void omitsDocumentLevelDiagnosticsWithoutSourceRanges() {
        EditorDiagnostic diagnostic = new EditorDiagnostic(
                EditorDiagnosticSeverity.WARNING, "java", "Project warning", "", Optional.empty(), Map.of());
        EditorDiagnosticSnapshot snapshot = new EditorDiagnosticSnapshot(
                new DiagnosticCollectionId("io.github.glynch.test.java"),
                URI.create("file:///Example.java"),
                diagnostic);

        assertThat(MonacoDiagnosticsJson.encode(List.of(snapshot))).isEqualTo("[]");
    }
}
