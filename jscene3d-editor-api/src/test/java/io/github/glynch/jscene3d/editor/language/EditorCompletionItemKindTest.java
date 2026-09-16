/*
 * Copyright 2026 Graham Lynch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EditorCompletionItemKindTest {

    @Test
    void definesSupportedCompletionKinds() {
        assertThat(EditorCompletionItemKind.values())
                .containsExactly(
                        EditorCompletionItemKind.TEXT,
                        EditorCompletionItemKind.METHOD,
                        EditorCompletionItemKind.FUNCTION,
                        EditorCompletionItemKind.CONSTRUCTOR,
                        EditorCompletionItemKind.FIELD,
                        EditorCompletionItemKind.VARIABLE,
                        EditorCompletionItemKind.CLASS,
                        EditorCompletionItemKind.INTERFACE,
                        EditorCompletionItemKind.MODULE,
                        EditorCompletionItemKind.PROPERTY,
                        EditorCompletionItemKind.UNIT,
                        EditorCompletionItemKind.VALUE,
                        EditorCompletionItemKind.ENUM,
                        EditorCompletionItemKind.KEYWORD,
                        EditorCompletionItemKind.SNIPPET,
                        EditorCompletionItemKind.COLOR,
                        EditorCompletionItemKind.FILE,
                        EditorCompletionItemKind.REFERENCE,
                        EditorCompletionItemKind.FOLDER,
                        EditorCompletionItemKind.ENUM_MEMBER,
                        EditorCompletionItemKind.CONSTANT,
                        EditorCompletionItemKind.STRUCT,
                        EditorCompletionItemKind.EVENT,
                        EditorCompletionItemKind.OPERATOR,
                        EditorCompletionItemKind.TYPE_PARAMETER);
    }
}
