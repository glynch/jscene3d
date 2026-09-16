/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.language.EditorCompletionItem;
import io.github.glynch.jscene3d.editor.language.EditorCompletionItemKind;
import io.github.glynch.jscene3d.editor.language.EditorTextEdit;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

class LspCompletionItemTranslatorTest {

    @Test
    void translatesStandardCompletionItem() {
        CompletionItem source = new CompletionItem("length()");
        source.setKind(CompletionItemKind.Method);
        source.setDetail("int String.length()");
        source.setDocumentation("Returns the length of this string.");
        source.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(2, 4), new Position(2, 7)), "length")));

        EditorCompletionItem result = LspCompletionItemTranslator.from(source);

        assertThat(result.label()).isEqualTo("length()");
        assertThat(result.kind()).contains(EditorCompletionItemKind.METHOD);
        assertThat(result.detail()).contains("int String.length()");
        assertThat(result.documentation()).contains("Returns the length of this string.");
        assertThat(result.textEdit())
                .contains(new EditorTextEdit(
                        new EditorTextRange(new EditorTextPosition(2, 4), new EditorTextPosition(2, 7)), "length"));
    }

    @Test
    void translatesMarkupDocumentationToText() {
        CompletionItem source = new CompletionItem("length()");
        MarkupContent documentation = new MarkupContent();
        documentation.setKind(MarkupKind.MARKDOWN);
        documentation.setValue("Returns the **length**.");
        source.setDocumentation(documentation);

        EditorCompletionItem result = LspCompletionItemTranslator.from(source);

        assertThat(result.documentation()).contains("Returns the **length**.");
    }

    @Test
    void supportsUnspecifiedOptionalProperties() {
        CompletionItem source = new CompletionItem("length()");

        EditorCompletionItem result = LspCompletionItemTranslator.from(source);

        assertThat(result.kind()).isEmpty();
        assertThat(result.detail()).isEmpty();
        assertThat(result.documentation()).isEmpty();
        assertThat(result.textEdit()).isEmpty();
    }

    @Test
    void defersInsertReplaceEdit() {
        CompletionItem source = new CompletionItem("length()");
        InsertReplaceEdit edit = new InsertReplaceEdit(
                "length",
                new Range(new Position(2, 4), new Position(2, 7)),
                new Range(new Position(2, 4), new Position(2, 10)));
        source.setTextEdit(Either.forRight(edit));

        EditorCompletionItem result = LspCompletionItemTranslator.from(source);

        assertThat(result.textEdit()).isEmpty();
    }
}
