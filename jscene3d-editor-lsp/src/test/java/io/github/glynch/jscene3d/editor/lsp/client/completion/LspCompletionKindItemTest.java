/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.lsp.client.completion;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.language.EditorCompletionItemKind;
import java.util.stream.Stream;
import org.eclipse.lsp4j.CompletionItemKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LspCompletionItemKindTest {

    @ParameterizedTest
    @MethodSource("completionKinds")
    void translatesCompletionKind(CompletionItemKind source, EditorCompletionItemKind expected) {
        assertThat(LspCompletionItemKind.from(source)).isEqualTo(expected);
    }

    @Test
    void translatesUnspecifiedKind() {
        assertThat(LspCompletionItemKind.from(null)).isNull();
    }

    private static Stream<Arguments> completionKinds() {
        return Stream.of(
                Arguments.of(CompletionItemKind.Text, EditorCompletionItemKind.TEXT),
                Arguments.of(CompletionItemKind.Method, EditorCompletionItemKind.METHOD),
                Arguments.of(CompletionItemKind.Function, EditorCompletionItemKind.FUNCTION),
                Arguments.of(CompletionItemKind.Constructor, EditorCompletionItemKind.CONSTRUCTOR),
                Arguments.of(CompletionItemKind.Field, EditorCompletionItemKind.FIELD),
                Arguments.of(CompletionItemKind.Variable, EditorCompletionItemKind.VARIABLE),
                Arguments.of(CompletionItemKind.Class, EditorCompletionItemKind.CLASS),
                Arguments.of(CompletionItemKind.Interface, EditorCompletionItemKind.INTERFACE),
                Arguments.of(CompletionItemKind.Module, EditorCompletionItemKind.MODULE),
                Arguments.of(CompletionItemKind.Property, EditorCompletionItemKind.PROPERTY),
                Arguments.of(CompletionItemKind.Unit, EditorCompletionItemKind.UNIT),
                Arguments.of(CompletionItemKind.Value, EditorCompletionItemKind.VALUE),
                Arguments.of(CompletionItemKind.Enum, EditorCompletionItemKind.ENUM),
                Arguments.of(CompletionItemKind.Keyword, EditorCompletionItemKind.KEYWORD),
                Arguments.of(CompletionItemKind.Snippet, EditorCompletionItemKind.SNIPPET),
                Arguments.of(CompletionItemKind.Color, EditorCompletionItemKind.COLOR),
                Arguments.of(CompletionItemKind.File, EditorCompletionItemKind.FILE),
                Arguments.of(CompletionItemKind.Reference, EditorCompletionItemKind.REFERENCE),
                Arguments.of(CompletionItemKind.Folder, EditorCompletionItemKind.FOLDER),
                Arguments.of(CompletionItemKind.EnumMember, EditorCompletionItemKind.ENUM_MEMBER),
                Arguments.of(CompletionItemKind.Constant, EditorCompletionItemKind.CONSTANT),
                Arguments.of(CompletionItemKind.Struct, EditorCompletionItemKind.STRUCT),
                Arguments.of(CompletionItemKind.Event, EditorCompletionItemKind.EVENT),
                Arguments.of(CompletionItemKind.Operator, EditorCompletionItemKind.OPERATOR),
                Arguments.of(CompletionItemKind.TypeParameter, EditorCompletionItemKind.TYPE_PARAMETER));
    }
}
