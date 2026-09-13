/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.activity.EditorActivityRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacementRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistry;
import io.github.glynch.jscene3d.editor.configuration.EditorConfiguration;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostics;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensions;
import io.github.glynch.jscene3d.editor.file.EditorFileTypes;
import io.github.glynch.jscene3d.editor.language.EditorLanguageSupports;
import io.github.glynch.jscene3d.editor.lifecycle.ExtensionSubscriptions;
import io.github.glynch.jscene3d.editor.menu.EditorMenuRegistry;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.status.EditorStatusBar;
import io.github.glynch.jscene3d.editor.theme.EditorColorThemes;
import io.github.glynch.jscene3d.editor.view.EditorViewRegistry;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.util.Objects;

/** Immutable extension-facing view of one editor window and one subscription lifetime. */
final class EditorExtensionScope implements EditorExtensionContext {
    record Capabilities(
            EditorActivityRegistry activities,
            EditorViewRegistry views,
            EditorExtensions extensions,
            EditorCommandRegistry commands,
            EditorCommandPlacementRegistry commandPlacements,
            EditorMenuRegistry menus,
            EditorConfiguration configuration,
            EditorStatusBar statusBar,
            EditorWindow window,
            EditorDiagnostics diagnostics,
            EditorFileTypes fileTypes,
            EditorLanguageSupports languageSupports,
            EditorColorThemes colorThemes,
            EditorProjects projects,
            EditorSelections selections) {
        Capabilities {
            Objects.requireNonNull(activities, "activities");
            Objects.requireNonNull(views, "views");
            Objects.requireNonNull(extensions, "extensions");
            Objects.requireNonNull(commands, "commands");
            Objects.requireNonNull(commandPlacements, "commandPlacements");
            Objects.requireNonNull(menus, "menus");
            Objects.requireNonNull(configuration, "configuration");
            Objects.requireNonNull(statusBar, "statusBar");
            Objects.requireNonNull(window, "window");
            Objects.requireNonNull(diagnostics, "diagnostics");
            Objects.requireNonNull(fileTypes, "fileTypes");
            Objects.requireNonNull(languageSupports, "languageSupports");
            Objects.requireNonNull(colorThemes, "colorThemes");
            Objects.requireNonNull(projects, "projects");
            Objects.requireNonNull(selections, "selections");
        }
    }

    private final Capabilities capabilities;
    private final ExtensionSubscriptions subscriptions;

    EditorExtensionScope(Capabilities capabilities, ExtensionSubscriptions subscriptions) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.subscriptions = Objects.requireNonNull(subscriptions, "subscriptions");
    }

    @Override
    public EditorActivityRegistry activities() {
        return capabilities.activities();
    }

    @Override
    public EditorViewRegistry views() {
        return capabilities.views();
    }

    @Override
    public EditorExtensions extensions() {
        return capabilities.extensions();
    }

    @Override
    public EditorCommandRegistry commands() {
        return capabilities.commands();
    }

    @Override
    public EditorCommandPlacementRegistry commandPlacements() {
        return capabilities.commandPlacements();
    }

    @Override
    public EditorMenuRegistry menus() {
        return capabilities.menus();
    }

    @Override
    public EditorConfiguration configuration() {
        return capabilities.configuration();
    }

    @Override
    public EditorStatusBar statusBar() {
        return capabilities.statusBar();
    }

    @Override
    public EditorWindow window() {
        return capabilities.window();
    }

    @Override
    public EditorDiagnostics diagnostics() {
        return capabilities.diagnostics();
    }

    @Override
    public EditorFileTypes fileTypes() {
        return capabilities.fileTypes();
    }

    @Override
    public EditorLanguageSupports languageSupports() {
        return capabilities.languageSupports();
    }

    @Override
    public EditorColorThemes colorThemes() {
        return capabilities.colorThemes();
    }

    @Override
    public EditorProjects projects() {
        return capabilities.projects();
    }

    @Override
    public EditorSelections selections() {
        return capabilities.selections();
    }

    @Override
    public ExtensionSubscriptions subscriptions() {
        return subscriptions;
    }
}
