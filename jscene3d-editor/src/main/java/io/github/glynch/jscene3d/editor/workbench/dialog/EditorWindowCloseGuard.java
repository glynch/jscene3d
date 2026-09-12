/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.dialog;

import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButton;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonRole;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Owns dirty-resource confirmation policy independently from its modal-dialog adapter. */
public final class EditorWindowCloseGuard {
    static final EditorDialogButtonId SAVE_AND_CLOSE =
            new EditorDialogButtonId("io.github.glynch.jscene3d.editor.dialog.save-and-close");
    static final EditorDialogButtonId DISCARD_AND_CLOSE =
            new EditorDialogButtonId("io.github.glynch.jscene3d.editor.dialog.discard-and-close");
    static final EditorDialogButtonId CANCEL_CLOSE =
            new EditorDialogButtonId("io.github.glynch.jscene3d.editor.dialog.cancel-close");

    private final Function<EditorDialog, Optional<EditorDialogButtonId>> dialogs;

    /** Creates a close guard using one synchronous modal-dialog presenter. */
    public EditorWindowCloseGuard(Function<EditorDialog, Optional<EditorDialogButtonId>> dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
    }

    /**
     * Returns whether closing may proceed after considering every dirty resource.
     *
     * @param projectName current project display name
     * @param dirtyCount number of dirty registered resources
     * @param saveAll saves every dirty resource and reports success
     * @return whether the caller may close the editor window
     */
    public boolean confirmClose(String projectName, int dirtyCount, BooleanSupplier saveAll) {
        if (dirtyCount < 0) {
            throw new IllegalArgumentException("dirtyCount must not be negative");
        }
        if (dirtyCount == 0) {
            return true;
        }
        String name = Objects.requireNonNull(projectName, "projectName");
        BooleanSupplier save = Objects.requireNonNull(saveAll, "saveAll");
        Optional<EditorDialogButtonId> selected = dialogs.apply(confirmation(name, dirtyCount));
        if (selected.filter(DISCARD_AND_CLOSE::equals).isPresent()) {
            return true;
        }
        return selected.filter(SAVE_AND_CLOSE::equals).isPresent() && save.getAsBoolean();
    }

    private static EditorDialog confirmation(String projectName, int dirtyCount) {
        String resourceLabel = dirtyCount == 1 ? "resource has" : "resources have";
        return new EditorDialog(
                "Unsaved Changes",
                "Save changes to " + projectName + "?",
                dirtyCount + " " + resourceLabel + " unsaved changes.",
                List.of(
                        new EditorDialogButton(SAVE_AND_CLOSE, "Save", EditorDialogButtonRole.DEFAULT),
                        new EditorDialogButton(DISCARD_AND_CLOSE, "Don't Save", EditorDialogButtonRole.DESTRUCTIVE),
                        new EditorDialogButton(CANCEL_CLOSE, "Cancel", EditorDialogButtonRole.CANCEL)));
    }
}
