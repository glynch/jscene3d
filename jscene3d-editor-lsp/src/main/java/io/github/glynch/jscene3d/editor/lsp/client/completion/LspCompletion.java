/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import io.github.glynch.jscene3d.editor.language.EditorCompletionItem;
import io.github.glynch.jscene3d.editor.language.EditorCompletionRequest;
import io.github.glynch.jscene3d.editor.language.EditorCompletionResult;
import java.util.List;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Provides language-server protocol translation for editor completion operations.
 *
 * <p>This class is the completion translation boundary used by the LSP client. Protocol-specific
 * translation details remain encapsulated within the completion package.
 */
public final class LspCompletion {

    private LspCompletion() {
        // Utility class.
    }

    /**
     * Creates language-server protocol parameters for an editor completion request.
     *
     * @param request editor completion request
     * @return corresponding language-server protocol parameters
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public static CompletionParams params(EditorCompletionRequest request) {
        return LspCompletionParams.from(request);
    }

    /**
     * Translates a language-server completion item into an editor completion item.
     *
     * @param item language-server completion item
     * @return corresponding editor completion item
     * @throws NullPointerException if {@code item} is {@code null}
     */
    public static EditorCompletionItem item(CompletionItem item) {
        return LspCompletionItemTranslator.from(item);
    }

    /**
     * Translates a language-server completion response into an editor completion result.
     *
     * @param version editor document version for which completion was requested
     * @param response language-server completion response
     * @return corresponding editor completion result
     * @throws NullPointerException if {@code response} is {@code null}
     */
    public static EditorCompletionResult result(int version, Either<List<CompletionItem>, CompletionList> response) {
        return LspCompletionResultTranslator.from(version, response);
    }
}
