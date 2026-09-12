/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.window;

import io.github.glynch.jscene3d.editor.view.ViewId;
import java.util.Optional;

/** Safe toolkit-independent interaction with the containing editor window. */
public interface EditorWindow {
    /**
     * Displays a transient user-facing message using workbench policy.
     *
     * @param message message to display
     */
    void showMessage(EditorMessage message);

    /**
     * Opens and focuses one registered editor view.
     *
     * @param view registered view identity
     * @throws IllegalArgumentException if no view has the supplied identity
     */
    void showView(ViewId view);

    /**
     * Shows one application-modal dialog and returns the selected action.
     *
     * @param dialog toolkit-independent dialog declaration
     * @return selected action, or empty when the user dismisses the dialog through its window chrome
     */
    Optional<EditorDialogButtonId> showDialog(EditorDialog dialog);
}
