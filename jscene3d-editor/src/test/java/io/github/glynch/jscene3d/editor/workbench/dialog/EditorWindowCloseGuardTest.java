/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonRole;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EditorWindowCloseGuardTest {
    @Test
    void cleanWorkspaceClosesWithoutOpeningDialog() {
        AtomicBoolean dialogShown = new AtomicBoolean();
        EditorWindowCloseGuard guard = new EditorWindowCloseGuard(dialog -> {
            dialogShown.set(true);
            return Optional.empty();
        });

        assertThat(guard.confirmClose("Doomed Corridors", 0, () -> false)).isTrue();
        assertThat(dialogShown).isFalse();
    }

    @Test
    void savesEveryDirtyResourceBeforeClosingWhenRequested() {
        AtomicReference<EditorDialog> shown = new AtomicReference<>();
        AtomicBoolean saved = new AtomicBoolean();
        EditorWindowCloseGuard guard = new EditorWindowCloseGuard(dialog -> {
            shown.set(dialog);
            return Optional.of(EditorWindowCloseGuard.SAVE_AND_CLOSE);
        });

        boolean close = guard.confirmClose("Doomed Corridors", 2, () -> {
            saved.set(true);
            return true;
        });

        assertThat(close).isTrue();
        assertThat(saved).isTrue();
        assertThat(shown.get().content()).isEqualTo("2 resources have unsaved changes.");
        assertThat(shown.get().buttons())
                .extracting(button -> button.role())
                .containsExactly(
                        EditorDialogButtonRole.DEFAULT,
                        EditorDialogButtonRole.DESTRUCTIVE,
                        EditorDialogButtonRole.CANCEL);
    }

    @Test
    void cancelDismissalAndFailedSaveKeepWindowOpenWhileDiscardCloses() {
        AtomicReference<Optional<EditorDialogButtonId>> result =
                new AtomicReference<>(Optional.of(EditorWindowCloseGuard.CANCEL_CLOSE));
        EditorWindowCloseGuard guard = new EditorWindowCloseGuard(ignored -> result.get());

        assertThat(guard.confirmClose("Project", 1, () -> true)).isFalse();

        result.set(Optional.empty());
        assertThat(guard.confirmClose("Project", 1, () -> true)).isFalse();

        result.set(Optional.of(EditorWindowCloseGuard.SAVE_AND_CLOSE));
        assertThat(guard.confirmClose("Project", 1, () -> false)).isFalse();

        result.set(Optional.of(EditorWindowCloseGuard.DISCARD_AND_CLOSE));
        assertThat(guard.confirmClose("Project", 1, () -> false)).isTrue();
    }

    @Test
    void rejectsNegativeDirtyCount() {
        EditorWindowCloseGuard guard = new EditorWindowCloseGuard(ignored -> Optional.empty());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> guard.confirmClose("Project", -1, () -> true))
                .withMessage("dirtyCount must not be negative");
    }
}
