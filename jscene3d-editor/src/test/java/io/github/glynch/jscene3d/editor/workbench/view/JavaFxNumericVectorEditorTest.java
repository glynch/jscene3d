/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises the toolkit-independent value conversion used by the XYZ editor. */
final class JavaFxNumericVectorEditorTest {
    /** Separates compact vector values into author-facing axis values. */
    @Test
    void separatesVectorComponents() {
        assertThat(JavaFxNumericVectorEditor.components("[-5, 0.875, 6]", 3)).containsExactly("-5", "0.875", "6");
    }

    /** Reassembles the three numeric fields into one atomic property replacement. */
    @Test
    void createsCompleteReplacement() {
        assertThat(JavaFxNumericVectorEditor.replacement(List.of(" -4 ", "0.875", "6")))
                .isEqualTo("[-4, 0.875, 6]");
    }

    /** Allows useful intermediate numeric states while rejecting non-numeric input. */
    @Test
    void filtersNumericEditingCharacters() {
        assertThat(List.of("", "+", "-", ".", "-.", "+5", "-5.25", ".5"))
                .allMatch(JavaFxNumericVectorEditor::acceptsPartialNumber);
        assertThat(List.of("x", "-5xxx", "--5", "1.2.3", "1e", "1e-", "1E+4", "1 e 2"))
                .noneMatch(JavaFxNumericVectorEditor::acceptsPartialNumber);
    }

    /** Distinguishes temporary editing states from complete project numbers. */
    @Test
    void requiresCompleteNumberOnCommit() {
        assertThat(List.of("+5", "-5.25", ".5")).allMatch(JavaFxNumericVectorEditor::isCompleteNumber);
        assertThat(List.of("", "+", "-", ".", "-.", "1e", "1e-", "1E+4"))
                .noneMatch(JavaFxNumericVectorEditor::isCompleteNumber);
    }

    /** Rejects malformed projected values instead of silently assigning the wrong axes. */
    @Test
    void rejectsWrongComponentCount() {
        assertThatThrownBy(() -> JavaFxNumericVectorEditor.components("[1, 2]", 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 components");
    }
}
