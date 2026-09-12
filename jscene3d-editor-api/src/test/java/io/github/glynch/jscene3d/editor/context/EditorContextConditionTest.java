/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Exercises typed editor-context identities and declarative equality conditions. */
final class EditorContextConditionTest {
    /** Matches only when the typed current value equals the declared requirement. */
    @Test
    void matchesTypedContextValues() {
        EditorContextCondition<Boolean> condition = EditorContextCondition.isTrue(EditorContextKeys.PROJECT_OPEN);
        EditorContextValues closedProject = values(Optional.of(false));
        EditorContextValues openProject = values(Optional.of(true));
        EditorContextValues unset = values(Optional.empty());

        assertThat(condition.matches(closedProject)).isFalse();
        assertThat(condition.matches(openProject)).isTrue();
        assertThat(condition.matches(unset)).isFalse();
    }

    /** Rejects context identities outside the stable lowercase dotted format. */
    @Test
    void rejectsInvalidContextKeys() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorContextKey<>("projectOpen", Boolean.class))
                .withMessageContaining("lowercase dotted identity");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EditorContextKey<>("jscene3d.project--open", Boolean.class));
    }

    private static EditorContextValues values(Optional<Boolean> projectOpen) {
        return new EditorContextValues() {
            @Override
            public <T> Optional<T> get(EditorContextKey<T> key) {
                if (!key.equals(EditorContextKeys.PROJECT_OPEN)) {
                    return Optional.empty();
                }
                return projectOpen.map(key.valueClass()::cast);
            }
        };
    }
}
