/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.glynch.jscene3d.editor.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class EditorCompletionTriggerTest {

    @Test
    void createsManualTrigger() {
        EditorCompletionTrigger trigger = EditorCompletionTrigger.manual();

        assertThat(trigger.kind()).isEqualTo(EditorCompletionTrigger.Kind.MANUAL);
        assertThat(trigger.triggerCharacter()).isEmpty();
    }

    @Test
    void createsCharacterTrigger() {
        EditorCompletionTrigger trigger = EditorCompletionTrigger.character(".");

        assertThat(trigger.kind()).isEqualTo(EditorCompletionTrigger.Kind.TRIGGER_CHARACTER);
        assertThat(trigger.triggerCharacter()).contains(".");
    }

    @Test
    void supportsSupplementaryUnicodeTriggerCharacter() {
        EditorCompletionTrigger trigger = EditorCompletionTrigger.character("\uD83D\uDE00");

        assertThat(trigger.triggerCharacter()).contains("\uD83D\uDE00");
    }

    @Test
    void rejectsNullTriggerCharacter() {
        assertThatNullPointerException()
                .isThrownBy(() -> EditorCompletionTrigger.character(null))
                .withMessage("triggerCharacter");
    }

    @Test
    void rejectsEmptyTriggerCharacter() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EditorCompletionTrigger.character(""))
                .withMessage("triggerCharacter must contain exactly one Unicode code point");
    }

    @Test
    void rejectsMultipleTriggerCharacters() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> EditorCompletionTrigger.character(".."))
                .withMessage("triggerCharacter must contain exactly one Unicode code point");
    }

    @Test
    void hasValueSemantics() {
        EditorCompletionTrigger first = EditorCompletionTrigger.character(".");
        EditorCompletionTrigger second = EditorCompletionTrigger.character(".");
        EditorCompletionTrigger different = EditorCompletionTrigger.character(":");

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second).isNotEqualTo(different);
    }

    @Test
    void formatsTriggerAsString() {
        assertThat(EditorCompletionTrigger.character("."))
                .hasToString("EditorCompletionTrigger[kind=TRIGGER_CHARACTER, triggerCharacter=.]");
    }
}
