/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.view;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

final class EditorViewContributionTest {
    private static final ViewContainerId CONTAINER = new ViewContainerId("io.github.glynch.jscene3d.editor.panel");

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void rejectsMissingViewOrContainer() {
        EditorView view = testView("Test");

        assertThatNullPointerException()
                .isThrownBy(() -> new EditorViewContribution(null, CONTAINER, 0))
                .withMessage("view");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorViewContribution(view, null, 0))
                .withMessage("container");
    }

    @Test
    @SuppressWarnings("NullAway") // Deliberate nulls verify public boundary validation.
    void rejectsInvalidViewMetadata() {
        ViewId viewId = new ViewId("io.github.glynch.test.view");
        ViewKindId kind = new ViewKindId("io.github.glynch.test.kind");
        EditorView nullId = new TestView(null, "Test", kind);
        EditorView nullTitle = new TestView(viewId, null, kind);
        EditorView blankTitle = new TestView(viewId, " \t", kind);
        EditorView nullKind = new TestView(viewId, "Test", null);

        assertThatNullPointerException()
                .isThrownBy(() -> new EditorViewContribution(nullId, CONTAINER, 0))
                .withMessage("view.id()");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorViewContribution(nullKind, CONTAINER, 0))
                .withMessage("view.kind()");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorViewContribution(nullTitle, CONTAINER, 0))
                .withMessage("view.title()");
        assertThatThrownBy(() -> new EditorViewContribution(blankTitle, CONTAINER, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("view title must not be blank");
    }

    private static EditorView testView(String title) {
        return new TestView(
                new ViewId("io.github.glynch.test.view"), title, new ViewKindId("io.github.glynch.test.kind"));
    }

    private record TestView(ViewId id, String title, ViewKindId kind) implements EditorView {}
}
