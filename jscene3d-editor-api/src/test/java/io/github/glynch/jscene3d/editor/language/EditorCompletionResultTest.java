/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EditorCompletionResultTest {

    private static final EditorCompletionItem ITEM =
            new EditorCompletionItem("length()", EditorCompletionItemKind.METHOD, null, null, null);

    @Test
    void exposesCompletionResultProperties() {
        EditorCompletionResult result = new EditorCompletionResult(7, List.of(ITEM), true);

        assertThat(result.version()).isEqualTo(7);
        assertThat(result.items()).containsExactly(ITEM);
        assertThat(result.incomplete()).isTrue();
    }

    @Test
    void supportsEmptyCompleteResult() {
        EditorCompletionResult result = new EditorCompletionResult(1, List.of(), false);

        assertThat(result.items()).isEmpty();
        assertThat(result.incomplete()).isFalse();
    }

    @Test
    void defensivelyCopiesItems() {
        List<EditorCompletionItem> items = new ArrayList<>();
        items.add(ITEM);

        EditorCompletionResult result = new EditorCompletionResult(1, items, false);
        items.clear();

        assertThat(result.items()).containsExactly(ITEM);
    }

    @Test
    void exposesImmutableItems() {
        EditorCompletionResult result = new EditorCompletionResult(1, List.of(ITEM), false);
        List<EditorCompletionItem> items = result.items();

        assertThatThrownBy(() -> items.add(ITEM)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullItems() {
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorCompletionResult(1, null, false))
                .withMessage("items");
    }

    @Test
    void rejectsNullItem() {
        List<EditorCompletionItem> items = new ArrayList<>();
        items.add(null);

        assertThatNullPointerException().isThrownBy(() -> new EditorCompletionResult(1, items, false));
    }

    @Test
    void rejectsZeroVersion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorCompletionResult(0, List.of(), false))
                .withMessage("version must be positive");
    }

    @Test
    void rejectsNegativeVersion() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorCompletionResult(-1, List.of(), false))
                .withMessage("version must be positive");
    }
}
