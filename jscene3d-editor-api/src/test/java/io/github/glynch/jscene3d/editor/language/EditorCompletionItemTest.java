/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.glynch.jscene3d.editor.diagnostic.EditorTextPosition;
import io.github.glynch.jscene3d.editor.diagnostic.EditorTextRange;
import org.junit.jupiter.api.Test;

class EditorCompletionItemTest {

    private static final EditorTextEdit TEXT_EDIT = new EditorTextEdit(
            new EditorTextRange(new EditorTextPosition(2, 4), new EditorTextPosition(2, 7)), "length");

    @Test
    void exposesCompletionProperties() {
        EditorCompletionItem item = new EditorCompletionItem(
                "length()",
                EditorCompletionItemKind.METHOD,
                "int String.length()",
                "Returns the length of this string.",
                TEXT_EDIT);

        assertThat(item.label()).isEqualTo("length()");
        assertThat(item.kind()).contains(EditorCompletionItemKind.METHOD);
        assertThat(item.detail()).contains("int String.length()");
        assertThat(item.documentation()).contains("Returns the length of this string.");
        assertThat(item.textEdit()).contains(TEXT_EDIT);
    }

    @Test
    void supportsUnspecifiedOptionalProperties() {
        EditorCompletionItem item = new EditorCompletionItem("length()", null, null, null, null);

        assertThat(item.kind()).isEmpty();
        assertThat(item.detail()).isEmpty();
        assertThat(item.documentation()).isEmpty();
        assertThat(item.textEdit()).isEmpty();
    }

    @Test
    void rejectsNullLabel() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCompletionItem(null, null, null, null, null))
                .withMessage("label");
    }

    @Test
    void rejectsEmptyLabel() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorCompletionItem("", null, null, null, null))
                .withMessage("label must not be empty");
    }
}
