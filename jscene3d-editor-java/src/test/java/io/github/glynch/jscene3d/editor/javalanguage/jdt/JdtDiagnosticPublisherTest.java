/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.javalanguage.jdt;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticSeverity;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

final class JdtDiagnosticPublisherTest {
    private static final URI SOURCE = URI.create("file:///workspace/Example.java");

    @Test
    void translatesCurrentJdtDiagnosticsAndRejectsStaleVersions() {
        RecordingCollection collection = new RecordingCollection();
        JdtDiagnosticPublisher publisher = new JdtDiagnosticPublisher(collection);
        publisher.documentVersion(SOURCE, 3);

        publisher.publish(publication(2, "stale"));
        assertThat(collection.diagnostics).isEmpty();

        publisher.publish(publication(3, "Missing semicolon"));

        assertThat(collection.diagnostics.get(SOURCE)).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.severity()).isEqualTo(EditorDiagnosticSeverity.ERROR);
            assertThat(diagnostic.code()).isEqualTo("1610612976");
            assertThat(diagnostic.source()).isEqualTo("Java");
            assertThat(diagnostic.message()).isEqualTo("Missing semicolon");
            assertThat(diagnostic.location()).isEqualTo("2:5");
            assertThat(diagnostic.range()).isPresent();
            assertThat(diagnostic.details()).isEmpty();
        });
    }

    @Test
    void closingADocumentClearsItsPublishedDiagnostics() {
        RecordingCollection collection = new RecordingCollection();
        JdtDiagnosticPublisher publisher = new JdtDiagnosticPublisher(collection);
        publisher.documentVersion(SOURCE, 1);
        publisher.publish(publication(1, "Broken"));

        publisher.documentClosed(SOURCE);
        publisher.publish(publication(1, "Late diagnostic"));

        assertThat(collection.diagnostics).doesNotContainKey(SOURCE);
    }

    private static PublishDiagnosticsParams publication(int version, String message) {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(new Position(1, 4), new Position(1, 10)));
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        diagnostic.setCode(Either.forRight(1610612976));
        diagnostic.setSource("Java");
        diagnostic.setMessage(message);
        PublishDiagnosticsParams publication = new PublishDiagnosticsParams(SOURCE.toString(), List.of(diagnostic));
        publication.setVersion(version);
        return publication;
    }

    private static final class RecordingCollection implements EditorDiagnosticCollection {
        private final Map<URI, List<EditorDiagnostic>> diagnostics = new LinkedHashMap<>();

        @Override
        public DiagnosticCollectionId id() {
            return new DiagnosticCollectionId("io.github.glynch.test.java");
        }

        @Override
        public void replace(URI source, List<EditorDiagnostic> replacement) {
            diagnostics.put(source, List.copyOf(replacement));
        }

        @Override
        public void replaceAll(Map<URI, List<EditorDiagnostic>> replacement) {
            diagnostics.clear();
            diagnostics.putAll(replacement);
        }

        @Override
        public void clear(URI source) {
            diagnostics.remove(source);
        }

        @Override
        public void clear() {
            diagnostics.clear();
        }

        @Override
        public void close() {
            diagnostics.clear();
        }
    }
}
