/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.editor.language.EditorCompletionItemKind;
import io.github.glynch.jscene3d.editor.language.EditorCompletionResult;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

class LspCompletionResultTranslatorTest {

    @Test
    void translatesCompletionItemList() {
        CompletionItem item = new CompletionItem("length()");
        item.setKind(CompletionItemKind.Method);
        Either<List<CompletionItem>, CompletionList> response = Either.forLeft(List.of(item));

        EditorCompletionResult result = LspCompletionResultTranslator.from(7, response);

        assertThat(result.version()).isEqualTo(7);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().label()).isEqualTo("length()");
        assertThat(result.items().getFirst().kind()).contains(EditorCompletionItemKind.METHOD);
        assertThat(result.incomplete()).isFalse();
    }

    @Test
    void translatesIncompleteCompletionList() {
        CompletionItem item = new CompletionItem("substring()");
        CompletionList completionList = new CompletionList(true, List.of(item));
        Either<List<CompletionItem>, CompletionList> response = Either.forRight(completionList);

        EditorCompletionResult result = LspCompletionResultTranslator.from(8, response);

        assertThat(result.version()).isEqualTo(8);
        assertThat(result.items()).extracting(itemResult -> itemResult.label()).containsExactly("substring()");
        assertThat(result.incomplete()).isTrue();
    }

    @Test
    void translatesEmptyCompletionList() {
        CompletionList completionList = new CompletionList(false, List.of());
        Either<List<CompletionItem>, CompletionList> response = Either.forRight(completionList);

        EditorCompletionResult result = LspCompletionResultTranslator.from(9, response);

        assertThat(result.version()).isEqualTo(9);
        assertThat(result.items()).isEmpty();
        assertThat(result.incomplete()).isFalse();
    }

    @Test
    void rejectsNullResponse() {
        assertThatNullPointerException()
                .isThrownBy(() -> LspCompletionResultTranslator.from(1, null))
                .withMessage("response");
    }
}
