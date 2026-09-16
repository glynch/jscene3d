/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import io.github.glynch.jscene3d.editor.language.EditorCompletionItem;
import io.github.glynch.jscene3d.editor.language.EditorTextEdit;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jspecify.annotations.Nullable;

/** Translates language-server completion items into editor completion items. */
final class LspCompletionItemTranslator {

    /** Prevents instantiation of this completion item translation utility class. */
    private LspCompletionItemTranslator() {
        throw new AssertionError("LspCompletionItemTranslator cannot be instantiated");
    }

    /**
     * Translates one language-server completion item.
     *
     * @param item language-server completion item
     * @return corresponding editor completion item
     * @throws NullPointerException if {@code item} is {@code null}
     */
    static EditorCompletionItem from(CompletionItem item) {
        return new EditorCompletionItem(
                item.getLabel(),
                LspCompletionItemKind.from(item.getKind()),
                item.getDetail(),
                documentation(item.getDocumentation()),
                textEdit(item.getTextEdit()));
    }

    private static @Nullable String documentation(@Nullable Either<String, MarkupContent> documentation) {
        if (documentation == null) {
            return null;
        }
        if (documentation.isLeft()) {
            return documentation.getLeft();
        }
        MarkupContent markup = documentation.getRight();
        return markup == null ? null : markup.getValue();
    }

    private static @Nullable EditorTextEdit textEdit(@Nullable Either<TextEdit, InsertReplaceEdit> textEdit) {
        if (textEdit == null || textEdit.isRight()) {
            return null;
        }

        TextEdit edit = textEdit.getLeft();
        if (edit == null) {
            return null;
        }

        return new EditorTextEdit(range(edit.getRange()), edit.getNewText());
    }

    private static EditorTextRange range(Range range) {
        return new EditorTextRange(position(range.getStart()), position(range.getEnd()));
    }

    private static EditorTextPosition position(Position position) {
        return new EditorTextPosition(position.getLine(), position.getCharacter());
    }
}
