/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.language.EditorCompletionItem;
import io.github.glynch.jscene3d.editor.language.EditorCompletionItemKind;
import io.github.glynch.jscene3d.editor.language.EditorCompletionRequest;
import io.github.glynch.jscene3d.editor.language.EditorCompletionTrigger;
import java.net.URI;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.junit.jupiter.api.Test;

class LspCompletionTest {

    @Test
    void translatesCompletionRequest() {
        EditorCompletionRequest request = new EditorCompletionRequest(
                URI.create("file:///workspace/Example.java"),
                7,
                new EditorTextPosition(3, 12),
                EditorCompletionTrigger.manual());

        CompletionParams result = LspCompletion.params(request);

        assertThat(result.getTextDocument().getUri())
                .isEqualTo(request.resource().toString());
        assertThat(result.getPosition().getLine()).isEqualTo(3);
        assertThat(result.getPosition().getCharacter()).isEqualTo(12);
        assertThat(result.getContext().getTriggerKind()).isEqualTo(CompletionTriggerKind.Invoked);
    }

    @Test
    void translatesCompletionItem() {
        CompletionItem source = new CompletionItem("length()");
        source.setKind(CompletionItemKind.Method);

        EditorCompletionItem result = LspCompletion.item(source);

        assertThat(result.label()).isEqualTo("length()");
        assertThat(result.kind()).contains(EditorCompletionItemKind.METHOD);
    }
}
