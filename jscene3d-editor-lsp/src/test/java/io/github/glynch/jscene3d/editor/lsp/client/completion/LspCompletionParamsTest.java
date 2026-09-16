/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.language.EditorCompletionRequest;
import io.github.glynch.jscene3d.editor.language.EditorCompletionTrigger;
import java.net.URI;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.junit.jupiter.api.Test;

class LspCompletionParamsTest {

    private static final URI RESOURCE = URI.create("file:///workspace/src/main/java/example/Example.java");

    @Test
    void translatesManualCompletionRequest() {
        EditorCompletionRequest request = new EditorCompletionRequest(
                RESOURCE, 7, new EditorTextPosition(3, 12), EditorCompletionTrigger.manual());

        CompletionParams parameters = LspCompletionParams.from(request);

        assertThat(parameters.getTextDocument().getUri()).isEqualTo(RESOURCE.toString());
        assertThat(parameters.getPosition().getLine()).isEqualTo(3);
        assertThat(parameters.getPosition().getCharacter()).isEqualTo(12);
        assertThat(parameters.getContext().getTriggerKind()).isEqualTo(CompletionTriggerKind.Invoked);
        assertThat(parameters.getContext().getTriggerCharacter()).isNull();
    }

    @Test
    void translatesTriggerCharacterCompletionRequest() {
        EditorCompletionRequest request = new EditorCompletionRequest(
                RESOURCE, 8, new EditorTextPosition(5, 9), EditorCompletionTrigger.character("."));

        CompletionParams parameters = LspCompletionParams.from(request);

        assertThat(parameters.getContext().getTriggerKind()).isEqualTo(CompletionTriggerKind.TriggerCharacter);
        assertThat(parameters.getContext().getTriggerCharacter()).isEqualTo(".");
    }

    @Test
    void rejectsNullRequest() {
        assertThatNullPointerException()
                .isThrownBy(() -> LspCompletionParams.from(null))
                .withMessage("request");
    }
}
