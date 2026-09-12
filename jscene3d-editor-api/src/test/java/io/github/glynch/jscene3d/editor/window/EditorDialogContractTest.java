/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

final class EditorDialogContractTest {
    private static final EditorDialogButtonId OK = new EditorDialogButtonId("io.github.glynch.test.dialog.ok");
    private static final EditorDialogButtonId CANCEL = new EditorDialogButtonId("io.github.glynch.test.dialog.cancel");

    @Test
    void preservesOrderedToolkitIndependentActions() {
        EditorDialog dialog = new EditorDialog(
                "Confirm",
                "Save changes?",
                "One resource has unsaved changes.",
                List.of(
                        new EditorDialogButton(OK, "Save", EditorDialogButtonRole.DEFAULT),
                        new EditorDialogButton(CANCEL, "Cancel", EditorDialogButtonRole.CANCEL)));

        assertThat(dialog.buttons()).extracting(EditorDialogButton::id).containsExactly(OK, CANCEL);
        assertThat(dialog.buttons().getFirst().role()).isEqualTo(EditorDialogButtonRole.DEFAULT);
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsAmbiguousOrMalformedDialogs() {
        EditorDialogButton primary = new EditorDialogButton(OK, "OK", EditorDialogButtonRole.DEFAULT);
        EditorDialogButton duplicatePrimary =
                new EditorDialogButton(CANCEL, "Continue", EditorDialogButtonRole.DEFAULT);
        List<EditorDialogButton> noButtons = List.of();
        List<EditorDialogButton> duplicateButtons = List.of(primary, primary);
        List<EditorDialogButton> duplicateDefaults = List.of(primary, duplicatePrimary);

        assertThatThrownBy(() -> new EditorDialog("Title", "Heading", "Content", noButtons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("buttons must not be empty");
        assertThatThrownBy(() -> new EditorDialog("Title", "Heading", "Content", duplicateButtons))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("buttons must not contain duplicate identities");
        assertThatThrownBy(() -> new EditorDialog("Title", "Heading", "Content", duplicateDefaults))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most one DEFAULT");
        assertThatNullPointerException()
                .isThrownBy(() -> new EditorDialogButton(null, "OK", EditorDialogButtonRole.DEFAULT))
                .withMessage("id");
    }
}
