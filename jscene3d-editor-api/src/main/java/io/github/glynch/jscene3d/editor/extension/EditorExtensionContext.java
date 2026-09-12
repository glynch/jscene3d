/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.extension;

import io.github.glynch.jscene3d.editor.activity.EditorActivityRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacementRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistry;
import io.github.glynch.jscene3d.editor.configuration.EditorConfiguration;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostics;
import io.github.glynch.jscene3d.editor.lifecycle.ExtensionSubscriptions;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.status.EditorStatusBar;
import io.github.glynch.jscene3d.editor.view.EditorViewRegistry;
import io.github.glynch.jscene3d.editor.window.EditorWindow;

/** Lifetime-scoped editor facilities supplied to one activated extension. */
public interface EditorExtensionContext {
    /**
     * Returns the registry for Activity Bar contributions.
     *
     * @return Activity Bar contribution registry
     */
    EditorActivityRegistry activities();

    /**
     * Returns the registry for logical editor views.
     *
     * @return view contribution registry
     */
    EditorViewRegistry views();

    /**
     * Returns the read-only catalogue of activated editor extensions.
     *
     * @return activated extension catalogue
     */
    EditorExtensions extensions();

    /**
     * Returns the registry for executable editor commands.
     *
     * @return command registry
     */
    EditorCommandRegistry commands();

    /**
     * Returns the registry which exposes commands on editor-owned interaction surfaces.
     *
     * @return command-placement registry
     */
    EditorCommandPlacementRegistry commandPlacements();

    /**
     * Returns effective project settings declared by core and installed extensions.
     *
     * @return read-only project configuration
     */
    EditorConfiguration configuration();

    /**
     * Returns the editor status-bar facility.
     *
     * @return status-bar facility
     */
    EditorStatusBar statusBar();

    /**
     * Returns safe interaction with the containing editor window.
     *
     * @return editor window facility
     */
    EditorWindow window();

    /**
     * Returns the facility for publishing extension-owned diagnostics.
     *
     * @return diagnostic collection factory
     */
    EditorDiagnostics diagnostics();

    /**
     * Returns the read-only lifecycle of the project opened in this editor window.
     *
     * @return current-project lifecycle
     */
    EditorProjects projects();

    /**
     * Returns the selection shared by editor views in this window.
     *
     * @return shared editor selection
     */
    EditorSelections selections();

    /**
     * Returns the registrations automatically closed when this extension is deactivated.
     *
     * @return extension-owned subscriptions
     */
    ExtensionSubscriptions subscriptions();
}
