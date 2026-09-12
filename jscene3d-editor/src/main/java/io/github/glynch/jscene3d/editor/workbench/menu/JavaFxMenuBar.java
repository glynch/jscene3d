/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.menu;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/** JavaFX adapter for toolkit-independent menu and command snapshots. */
public final class JavaFxMenuBar implements AutoCloseable {
    private final EditorExtensionHost extensions;
    private final MenuBar root = new MenuBar();
    private final EditorRegistration registration;

    /** Creates a menu bar which follows contributed menus, placements, and command state. */
    public JavaFxMenuBar(EditorExtensionHost extensions) {
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        root.getStyleClass().add(EditorStyleClasses.EDITOR_MENU_BAR);
        registration = extensions.observeMenus(this::showMenus);
    }

    /** Returns the workbench-owned menu-bar node. */
    public MenuBar node() {
        return root;
    }

    @Override
    public void close() {
        registration.close();
        root.getMenus().clear();
    }

    private void showMenus(List<EditorMenuSnapshot> menus) {
        root.getMenus().setAll(menus.stream().map(this::createMenu).toList());
    }

    private Menu createMenu(EditorMenuSnapshot snapshot) {
        Menu menu = new Menu(snapshot.contribution().title());
        String previousGroup = null;
        for (EditorMenuCommandSnapshot command : snapshot.commands()) {
            if (previousGroup != null && !previousGroup.equals(command.group())) {
                menu.getItems().add(new SeparatorMenuItem());
            }
            menu.getItems().add(createItem(command));
            previousGroup = command.group();
        }
        return menu;
    }

    private MenuItem createItem(EditorMenuCommandSnapshot snapshot) {
        MenuItem item = new MenuItem(snapshot.contribution().title());
        item.setDisable(!snapshot.state().enabled());
        item.setOnAction(ignored -> extensions.execute(snapshot.contribution().id()));
        accelerator(snapshot.contribution().id()).ifPresent(item::setAccelerator);
        return item;
    }

    private static Optional<KeyCombination> accelerator(CommandId command) {
        if (command.equals(EditorCommands.OPEN_PROJECT)) {
            return Optional.of(shortcut(KeyCode.O));
        }
        if (command.equals(EditorCommands.SAVE)) {
            return Optional.of(shortcut(KeyCode.S));
        }
        if (command.equals(EditorCommands.UNDO)) {
            return Optional.of(shortcut(KeyCode.Z));
        }
        if (command.equals(EditorCommands.REDO)) {
            return Optional.of(
                    new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        }
        if (command.equals(EditorCommands.OPEN_SETTINGS)) {
            return Optional.of(shortcut(KeyCode.COMMA));
        }
        if (command.equals(EditorCommands.QUIT)) {
            return Optional.of(shortcut(KeyCode.Q));
        }
        return Optional.empty();
    }

    private static KeyCombination shortcut(KeyCode key) {
        return new KeyCodeCombination(key, KeyCombination.SHORTCUT_DOWN);
    }
}
