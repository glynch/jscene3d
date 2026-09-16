/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import io.github.glynch.jscene3d.editor.language.EditorCompletionItem;
import io.github.glynch.jscene3d.editor.language.EditorCompletionResult;
import java.util.List;
import java.util.Objects;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/** Translates language-server completion responses into editor completion results. */
final class LspCompletionResultTranslator {

    /** Prevents instantiation of this completion result translation utility class. */
    private LspCompletionResultTranslator() {
        throw new AssertionError("LspCompletionResultTranslator cannot be instantiated");
    }

    /**
     * Translates a language-server completion response.
     *
     * @param version editor document version for which completion was requested
     * @param response language-server completion response
     * @return corresponding editor completion result
     * @throws NullPointerException if {@code response} is {@code null}
     */
    static EditorCompletionResult from(int version, Either<List<CompletionItem>, CompletionList> response) {
        Objects.requireNonNull(response, "response");

        if (response.isLeft()) {
            return result(version, response.getLeft(), false);
        }

        CompletionList completionList = response.getRight();
        return result(version, completionList.getItems(), completionList.isIncomplete());
    }

    private static EditorCompletionResult result(int version, List<CompletionItem> items, boolean incomplete) {
        List<EditorCompletionItem> translatedItems =
                items.stream().map(LspCompletionItemTranslator::from).toList();
        return new EditorCompletionResult(version, translatedItems, incomplete);
    }
}
