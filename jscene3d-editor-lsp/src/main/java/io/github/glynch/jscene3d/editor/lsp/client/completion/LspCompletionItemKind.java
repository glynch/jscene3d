/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import io.github.glynch.jscene3d.editor.language.EditorCompletionItemKind;
import org.eclipse.lsp4j.CompletionItemKind;
import org.jspecify.annotations.Nullable;

/** Translates language-server completion item kinds into editor completion item kinds. */
final class LspCompletionItemKind {

    /** Prevents instantiation of this completion item kind utility class. */
    private LspCompletionItemKind() {
        throw new AssertionError("LspCompletionItemKind cannot be instantiated");
    }

    /**
     * Translates a language-server completion item kind.
     *
     * @param kind language-server completion item kind, or {@code null} when unspecified
     * @return corresponding editor completion item kind, or {@code null} when unspecified
     */
    static @Nullable EditorCompletionItemKind from(@Nullable CompletionItemKind kind) {
        if (kind == null) {
            return null;
        }
        return switch (kind) {
            case Text -> EditorCompletionItemKind.TEXT;
            case Method -> EditorCompletionItemKind.METHOD;
            case Function -> EditorCompletionItemKind.FUNCTION;
            case Constructor -> EditorCompletionItemKind.CONSTRUCTOR;
            case Field -> EditorCompletionItemKind.FIELD;
            case Variable -> EditorCompletionItemKind.VARIABLE;
            case Class -> EditorCompletionItemKind.CLASS;
            case Interface -> EditorCompletionItemKind.INTERFACE;
            case Module -> EditorCompletionItemKind.MODULE;
            case Property -> EditorCompletionItemKind.PROPERTY;
            case Unit -> EditorCompletionItemKind.UNIT;
            case Value -> EditorCompletionItemKind.VALUE;
            case Enum -> EditorCompletionItemKind.ENUM;
            case Keyword -> EditorCompletionItemKind.KEYWORD;
            case Snippet -> EditorCompletionItemKind.SNIPPET;
            case Color -> EditorCompletionItemKind.COLOR;
            case File -> EditorCompletionItemKind.FILE;
            case Reference -> EditorCompletionItemKind.REFERENCE;
            case Folder -> EditorCompletionItemKind.FOLDER;
            case EnumMember -> EditorCompletionItemKind.ENUM_MEMBER;
            case Constant -> EditorCompletionItemKind.CONSTANT;
            case Struct -> EditorCompletionItemKind.STRUCT;
            case Event -> EditorCompletionItemKind.EVENT;
            case Operator -> EditorCompletionItemKind.OPERATOR;
            case TypeParameter -> EditorCompletionItemKind.TYPE_PARAMETER;
        };
    }
}
