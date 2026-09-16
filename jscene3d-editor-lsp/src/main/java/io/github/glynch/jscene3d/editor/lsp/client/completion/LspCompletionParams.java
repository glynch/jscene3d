/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import io.github.glynch.jscene3d.editor.language.EditorCompletionRequest;
import io.github.glynch.jscene3d.editor.language.EditorCompletionTrigger;
import java.util.Objects;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

/** Translates editor completion requests into language-server protocol parameters. */
final class LspCompletionParams {

    /** Prevents instantiation of this completion parameters utility class. */
    private LspCompletionParams() {
        throw new AssertionError("LspCompletionParams cannot be instantiated");
    }

    /**
     * Creates protocol completion parameters for an editor completion request.
     *
     * @param request editor completion request
     * @return corresponding language-server protocol parameters
     */
    static CompletionParams from(EditorCompletionRequest request) {
        EditorCompletionRequest current = Objects.requireNonNull(request, "request");
        CompletionParams parameters = new CompletionParams();
        parameters.setTextDocument(new TextDocumentIdentifier(current.resource().toString()));
        parameters.setPosition(
                new Position(current.position().line(), current.position().character()));
        parameters.setContext(context(current.trigger()));
        return parameters;
    }

    private static CompletionContext context(EditorCompletionTrigger trigger) {
        if (trigger.kind() == EditorCompletionTrigger.Kind.MANUAL) {
            return new CompletionContext(CompletionTriggerKind.Invoked);
        }
        return new CompletionContext(
                CompletionTriggerKind.TriggerCharacter,
                trigger.triggerCharacter().orElseThrow());
    }
}
