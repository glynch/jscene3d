/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.util.Optional;
import java.util.function.Consumer;

/** Read-only lifecycle of the project opened in the editor window. */
public interface EditorProjects {
    /**
     * Returns the current project.
     *
     * @return opened project, or empty while no project is open
     */
    Optional<EditorProject> current();

    /**
     * Observes project open and close transitions.
     *
     * <p>The listener immediately receives the current state.
     *
     * @param listener synchronous project-state listener
     * @return removable listener registration
     */
    EditorRegistration observe(Consumer<Optional<EditorProject>> listener);
}
