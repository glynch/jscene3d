/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.command;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.CommandLocationId;
import io.github.glynch.jscene3d.editor.command.EditorCommand;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandLocations;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistration;
import io.github.glynch.jscene3d.editor.command.EditorCommandState;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButton;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonRole;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Registers the editor-owned menus and commands behind one lifecycle handle. */
public final class EditorWorkbenchCommandSet implements AutoCloseable {
    private static final EditorDialogButtonId ABOUT_OK =
            new EditorDialogButtonId("io.github.glynch.jscene3d.editor.dialog.about-ok");

    private final List<EditorRegistration> registrations = new ArrayList<>();
    private final EditorCommandRegistration settings;
    private final EditorCommandRegistration save;
    private final EditorCommandRegistration undo;
    private final EditorCommandRegistration redo;

    /** Registers all core menus, commands, and placements. */
    public EditorWorkbenchCommandSet(EditorExtensionHost extensions, Actions actions, String version) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        Actions workbench = Objects.requireNonNull(actions, "actions");
        String editorVersion = Objects.requireNonNull(version, "version");

        registerMenus(host);
        register(
                host,
                EditorCommands.SHOW_ABOUT,
                "About JScene3D",
                context -> context.window().showDialog(aboutDialog(editorVersion)));
        settings = register(
                host,
                EditorCommands.OPEN_SETTINGS,
                "Settings…",
                ignored -> workbench.openSettings().run());
        register(
                host,
                EditorCommands.QUIT,
                "Quit JScene3D",
                ignored -> workbench.quit().run());
        register(
                host,
                EditorCommands.OPEN_PROJECT,
                "Open Project…",
                ignored -> workbench.openProject().run());
        save = register(
                host, EditorCommands.SAVE, "Save", ignored -> workbench.save().run());
        undo = register(
                host, EditorCommands.UNDO, "Undo", ignored -> workbench.undo().run());
        redo = register(
                host, EditorCommands.REDO, "Redo", ignored -> workbench.redo().run());
        registerPlacements(host);
        update(DocumentCommandState.EMPTY);
    }

    /** Replaces project-dependent command enablement as one coherent state. */
    public void update(DocumentCommandState state) {
        DocumentCommandState current = Objects.requireNonNull(state, "state");
        settings.update(enabled(current.projectOpen()));
        save.update(enabled(current.dirty()));
        undo.update(enabled(current.canUndo()));
        redo.update(enabled(current.canRedo()));
    }

    @Override
    public void close() {
        List.copyOf(registrations).reversed().forEach(EditorRegistration::close);
        registrations.clear();
    }

    private void registerMenus(EditorExtensionHost host) {
        registrations.add(
                host.registerMenu(new EditorMenuContribution(EditorCommandLocations.JSCENE3D_MENU, "JScene3D", 10)));
        registrations.add(host.registerMenu(new EditorMenuContribution(EditorCommandLocations.FILE_MENU, "File", 20)));
        registrations.add(host.registerMenu(new EditorMenuContribution(EditorCommandLocations.EDIT_MENU, "Edit", 30)));
    }

    private EditorCommandRegistration register(
            EditorExtensionHost host, CommandId id, String title, EditorCommand command) {
        EditorCommandRegistration registration =
                host.registerCommand(new EditorCommandContribution(id, title), command);
        registrations.add(registration);
        return registration;
    }

    private void registerPlacements(EditorExtensionHost host) {
        place(host, EditorCommands.SHOW_ABOUT, EditorCommandLocations.JSCENE3D_MENU, "application", 10);
        place(host, EditorCommands.OPEN_SETTINGS, EditorCommandLocations.JSCENE3D_MENU, "application", 20);
        place(host, EditorCommands.QUIT, EditorCommandLocations.JSCENE3D_MENU, "lifecycle", 30);
        place(host, EditorCommands.OPEN_PROJECT, EditorCommandLocations.FILE_MENU, "file", 10);
        place(host, EditorCommands.SAVE, EditorCommandLocations.FILE_MENU, "file", 20);
        place(host, EditorCommands.UNDO, EditorCommandLocations.EDIT_MENU, "history", 10);
        place(host, EditorCommands.REDO, EditorCommandLocations.EDIT_MENU, "history", 20);
    }

    private void place(
            EditorExtensionHost host, CommandId command, CommandLocationId location, String group, int order) {
        registrations.add(host.registerCommandPlacement(new EditorCommandPlacement(command, location, group, order)));
    }

    private static EditorCommandState enabled(boolean enabled) {
        return enabled ? EditorCommandState.ENABLED_STATE : EditorCommandState.DISABLED_STATE;
    }

    private static EditorDialog aboutDialog(String version) {
        String content = "Version: " + version + System.lineSeparator()
                + "Java: " + System.getProperty("java.version") + System.lineSeparator()
                + "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " "
                + System.getProperty("os.version");
        return new EditorDialog(
                "About JScene3D",
                "JScene3D Editor",
                content,
                List.of(new EditorDialogButton(ABOUT_OK, "OK", EditorDialogButtonRole.DEFAULT)));
    }

    /** Workbench behavior invoked by the core command set. */
    public record Actions(
            Runnable openProject, Runnable openSettings, Runnable save, Runnable undo, Runnable redo, Runnable quit) {
        /** Validates every required workbench action. */
        public Actions {
            Objects.requireNonNull(openProject, "openProject");
            Objects.requireNonNull(openSettings, "openSettings");
            Objects.requireNonNull(save, "save");
            Objects.requireNonNull(undo, "undo");
            Objects.requireNonNull(redo, "redo");
            Objects.requireNonNull(quit, "quit");
        }
    }

    /** Project-dependent command state supplied by the workspace. */
    public record DocumentCommandState(boolean projectOpen, boolean dirty, boolean canUndo, boolean canRedo) {
        /** State used before a project is opened. */
        public static final DocumentCommandState EMPTY = new DocumentCommandState(false, false, false, false);
    }
}
