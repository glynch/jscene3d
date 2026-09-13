/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.language;

import io.github.glynch.jscene3d.editor.project.EditorProject;

/** Opens project-scoped language intelligence without exposing its protocol or toolkit. */
@FunctionalInterface
public interface EditorLanguageSupport {
    /**
     * Opens language support for one project.
     *
     * <p>The returned session may initialize asynchronously. The caller owns it and closes it when the project is no
     * longer active.
     *
     * @param project opened editor project
     * @return owned project language session
     */
    EditorLanguageProjectSession openProject(EditorProject project);
}
