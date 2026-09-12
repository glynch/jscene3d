/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.opening;

import io.github.glynch.jscene3d.editor.EditorProjectSession;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.EditorProject;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Publishes one project-opening result to extension-facing project and diagnostic state. */
public final class EditorProjectPublication {
    private final EditorProjectContext projects;
    private final Consumer<List<ProjectDiagnostic>> diagnostics;
    private final EditorConfigurationContext configuration;
    private EditorRegistration documentRegistration = () -> {};

    /**
     * Creates a publisher over the editor's project and diagnostic contexts.
     *
     * @param projects extension-facing current-project context
     * @param diagnostics diagnostic publication sink
     */
    public EditorProjectPublication(EditorProjectContext projects, Consumer<List<ProjectDiagnostic>> diagnostics) {
        this(projects, diagnostics, new EditorConfigurationContext());
    }

    /** Creates a publisher over project, diagnostic, and effective-configuration contexts. */
    public EditorProjectPublication(
            EditorProjectContext projects,
            Consumer<List<ProjectDiagnostic>> diagnostics,
            EditorConfigurationContext configuration) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    /** Clears the current extension-facing project. */
    public void clearProject() {
        documentRegistration.close();
        documentRegistration = () -> {};
        configuration.clearProject();
        projects.clear();
    }

    /**
     * Publishes the loaded project, hierarchy, and assets atomically.
     *
     * @param session completely loaded editor project session
     */
    public void showProject(EditorProjectSession session) {
        EditorProjectSession loaded = Objects.requireNonNull(session, "session");
        configuration.showProject(loaded);
        projects.showProject(
                new EditorProject(
                        loaded.project().identity().id(),
                        loaded.project().identity().name(),
                        loaded.project().root().toUri()),
                loaded.hierarchy(),
                loaded.assets());
        documentRegistration.close();
        documentRegistration = loaded.onDidChangeHierarchy().subscribe(projects::updateHierarchy);
    }

    /**
     * Replaces all project-owned diagnostics.
     *
     * @param replacement complete diagnostic set
     */
    public void showDiagnostics(List<ProjectDiagnostic> replacement) {
        diagnostics.accept(List.copyOf(Objects.requireNonNull(replacement, "replacement")));
    }
}
