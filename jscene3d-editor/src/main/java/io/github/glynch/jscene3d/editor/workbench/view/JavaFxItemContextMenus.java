/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuCommandSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/** Builds JavaFX item context menus from toolkit-independent command placements. */
final class JavaFxItemContextMenus {
    private final Function<Optional<String>, List<EditorMenuCommandSnapshot>> commandSource;
    private final BiConsumer<CommandId, Object> commandExecutor;

    JavaFxItemContextMenus(
            Function<Optional<String>, List<EditorMenuCommandSnapshot>> commandSource,
            BiConsumer<CommandId, Object> commandExecutor) {
        this.commandSource = Objects.requireNonNull(commandSource, "commandSource");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor");
    }

    Optional<ContextMenu> create(Optional<String> contextValue, Object argument) {
        List<EditorMenuCommandSnapshot> commands =
                List.copyOf(commandSource.apply(Objects.requireNonNull(contextValue, "contextValue")));
        Object invocationArgument = Objects.requireNonNull(argument, "argument");
        if (commands.isEmpty()) {
            return Optional.empty();
        }
        ContextMenu menu = new ContextMenu();
        String previousGroup = "";
        for (EditorMenuCommandSnapshot command : commands) {
            if (!previousGroup.isEmpty() && !previousGroup.equals(command.group())) {
                menu.getItems().add(new SeparatorMenuItem());
            }
            menu.getItems().add(createItem(command, invocationArgument));
            previousGroup = command.group();
        }
        return Optional.of(menu);
    }

    private MenuItem createItem(EditorMenuCommandSnapshot snapshot, Object argument) {
        MenuItem item = new MenuItem(snapshot.contribution().title());
        item.setDisable(!snapshot.state().enabled());
        item.setOnAction(
                ignored -> commandExecutor.accept(snapshot.contribution().id(), argument));
        return item;
    }
}
