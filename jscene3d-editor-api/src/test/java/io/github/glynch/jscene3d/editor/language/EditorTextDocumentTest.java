/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.file.EditorLanguageId;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies versioned text-document snapshots and changes. */
final class EditorTextDocumentTest {
    @Test
    void createsDocumentsAndNonEmptyChanges() {
        EditorTextDocument document = document();
        EditorTextEdit edit = new EditorTextEdit(
                new EditorTextRange(new EditorTextPosition(0, 0), new EditorTextPosition(0, 1)), "b");

        EditorTextDocumentChange change = new EditorTextDocumentChange(document, List.of(edit));

        assertThat(change.document()).isEqualTo(document);
        assertThat(change.edits()).containsExactly(edit);
    }

    @Test
    void rejectsInvalidDocumentVersionsAndEmptyChanges() {
        URI source = URI.create("file:///Example.java");
        EditorLanguageId language = new EditorLanguageId("java");
        String text = "class Example {}";
        List<EditorTextEdit> noEdits = List.of();

        assertThatThrownBy(() -> new EditorTextDocument(source, language, 0, text))
                .isInstanceOf(IllegalArgumentException.class);
        EditorTextDocument document = document();
        assertThatThrownBy(() -> new EditorTextDocumentChange(document, noEdits))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static EditorTextDocument document() {
        return new EditorTextDocument(
                URI.create("file:///Example.java"), new EditorLanguageId("java"), 1, "class Example {}");
    }
}
