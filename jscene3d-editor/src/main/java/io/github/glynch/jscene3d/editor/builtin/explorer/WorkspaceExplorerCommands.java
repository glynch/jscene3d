/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.builtin.explorer;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.CommandLocationId;
import io.github.glynch.jscene3d.editor.command.EditorCommandContext;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandLocations;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import io.github.glynch.jscene3d.environment.OperatingSystem;
import java.util.Objects;

/** Registers item-oriented commands owned by the Workspace Explorer extension. */
final class WorkspaceExplorerCommands {
    static final CommandId OPEN_FILE = new CommandId("io.github.glynch.jscene3d.editor.workspace-explorer.open-file");

    private static final CommandId REVEAL_IN_FILE_MANAGER =
            new CommandId("io.github.glynch.jscene3d.editor.workspace-explorer.reveal-in-file-manager");
    private static final String WORKSPACE_FILE = "workspace-file";

    private final WorkspaceFileManager fileManager;

    WorkspaceExplorerCommands() {
        this(new WorkspaceFileManager(OperatingSystem.current()));
    }

    WorkspaceExplorerCommands(WorkspaceFileManager fileManager) {
        this.fileManager = Objects.requireNonNull(fileManager, "fileManager");
    }

    void register(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions()
                .add(editor.commands()
                        .register(
                                new EditorCommandContribution(OPEN_FILE, "Open"),
                                invocation -> openFile(invocation, editor.window())));
        editor.subscriptions()
                .add(editor.commands()
                        .register(
                                new EditorCommandContribution(REVEAL_IN_FILE_MANAGER, fileManager.revealCommandTitle()),
                                invocation -> reveal(invocation, editor.window())));
        CommandLocationId itemContext = EditorCommandLocations.viewItemContext(WorkspaceExplorerExtension.VIEW_ID);
        editor.subscriptions()
                .add(editor.commandPlacements()
                        .register(
                                new EditorCommandPlacement(OPEN_FILE, itemContext, "navigation", 10, WORKSPACE_FILE)));
        editor.subscriptions()
                .add(editor.commandPlacements()
                        .register(new EditorCommandPlacement(REVEAL_IN_FILE_MANAGER, itemContext, "filesystem", 20)));
    }

    private static void openFile(EditorCommandContext invocation, EditorWindow window) {
        invocation
                .argument(WorkspaceExplorerEntry.class)
                .filter(entry -> entry.kind() == WorkspaceExplorerEntry.Kind.FILE)
                .ifPresent(entry -> window.openFile(entry.path().toUri()));
    }

    private void reveal(EditorCommandContext invocation, EditorWindow window) {
        invocation.argument(WorkspaceExplorerEntry.class).ifPresent(entry -> {
            try {
                fileManager.reveal(entry.path());
            } catch (UnsupportedOperationException | SecurityException exception) {
                window.showMessage(new EditorMessage(
                        EditorMessageSeverity.ERROR,
                        "Unable to reveal " + entry.label() + ": "
                                + Objects.requireNonNullElse(exception.getMessage(), exception.toString())));
            }
        });
    }
}
