/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.CommandLocationId;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandLocations;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonBehavior;
import io.github.glynch.jscene3d.editor.workbench.command.EditorWorkbenchCommandSet.DocumentCommandState;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuCommandSnapshot;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuSnapshot;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Verifies the complete toolkit-independent core menu and command model. */
class EditorWorkbenchCommandSetTest {
    @Test
    void contributesCoreMenusAndUpdatesSharedCommandEnablement() {
        EditorExtensionHost host = host();
        AtomicInteger opens = new AtomicInteger();
        AtomicInteger settings = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        AtomicInteger undoes = new AtomicInteger();
        AtomicInteger redoes = new AtomicInteger();
        AtomicInteger quits = new AtomicInteger();
        AtomicReference<EditorDialog> dialog = new AtomicReference<>();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();
        host.showDialogsWith(request -> {
            dialog.set(request);
            return Optional.of(request.buttons().getFirst().id());
        });
        host.observeMenus(snapshots::add);

        String aboutText = "Version: 0.1.0-test" + System.lineSeparator()
                + "Commit: abc123" + System.lineSeparator()
                + "Build date: 2026-09-12T00:00:00Z";
        EditorWorkbenchCommandSet commands = new EditorWorkbenchCommandSet(
                host,
                new EditorWorkbenchCommandSet.Actions(
                        opens::incrementAndGet,
                        settings::incrementAndGet,
                        saves::incrementAndGet,
                        undoes::incrementAndGet,
                        redoes::incrementAndGet,
                        quits::incrementAndGet),
                aboutText);

        assertThat(snapshots.getLast())
                .extracting(menu -> menu.contribution().title())
                .containsExactly("JScene3D", "File", "Edit");
        assertThat(menu(snapshots.getLast(), "JScene3D").commands())
                .extracting(item -> item.contribution().title())
                .containsExactly("About JScene3D", "Settings…", "Quit JScene3D");
        assertThat(menu(snapshots.getLast(), "JScene3D").commands())
                .extracting(EditorMenuCommandSnapshot::group)
                .containsExactly("application", "application", "lifecycle");
        assertThat(command(snapshots.getLast(), EditorCommands.OPEN_PROJECT)
                        .state()
                        .enabled())
                .isTrue();
        assertThat(command(snapshots.getLast(), EditorCommands.SAVE).state().enabled())
                .isFalse();
        assertThat(command(snapshots.getLast(), EditorCommands.OPEN_SETTINGS)
                        .state()
                        .enabled())
                .isFalse();

        assertThatThrownBy(() -> host.execute(EditorCommands.SAVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");

        commands.update(new DocumentCommandState(true, true, true, true));
        host.execute(EditorCommands.OPEN_PROJECT);
        host.execute(EditorCommands.OPEN_SETTINGS);
        host.execute(EditorCommands.SAVE);
        host.execute(EditorCommands.UNDO);
        host.execute(EditorCommands.REDO);
        host.execute(EditorCommands.SHOW_ABOUT);
        host.execute(EditorCommands.QUIT);

        assertThat(opens).hasValue(1);
        assertThat(settings).hasValue(1);
        assertThat(saves).hasValue(1);
        assertThat(undoes).hasValue(1);
        assertThat(redoes).hasValue(1);
        assertThat(quits).hasValue(1);
        assertThat(dialog.get().title()).isEqualTo("About JScene3D Editor");
        assertThat(dialog.get().heading()).isEqualTo("JScene3D Editor");
        assertThat(dialog.get().content()).isEqualTo(aboutText);
        assertThat(dialog.get().buttons())
                .extracting(button -> button.title(), button -> button.behavior())
                .containsExactly(
                        tuple("OK", EditorDialogButtonBehavior.CLOSE),
                        tuple("Copy", EditorDialogButtonBehavior.COPY_CONTENT));

        commands.close();
        assertThat(snapshots.getLast()).isEmpty();
        host.close();
    }

    @Test
    void extensionCanAddToExistingMenuAndDeclareItsOwnTopLevelMenu() {
        EditorExtensionHost host = host();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();
        host.observeMenus(snapshots::add);
        EditorWorkbenchCommandSet commands = new EditorWorkbenchCommandSet(host, emptyActions(), "test");

        host.activate(menuExtension());

        assertThat(menu(snapshots.getLast(), "File").commands())
                .extracting(item -> item.contribution().title())
                .contains("Import Test Asset");
        assertThat(menu(snapshots.getLast(), "Test Tools").commands())
                .extracting(item -> item.contribution().title())
                .containsExactly("Import Test Asset");

        commands.close();
        host.close();
    }

    private static EditorExtension menuExtension() {
        CommandId command = new CommandId("io.github.glynch.test.import-asset");
        CommandLocationId menu = new CommandLocationId("io.github.glynch.test.tools-menu");
        return new EditorExtension() {
            @Override
            public String id() {
                return "io.github.glynch.test.menus";
            }

            @Override
            public void activate(EditorExtensionContext context) {
                context.subscriptions()
                        .add(context.commands()
                                .register(new EditorCommandContribution(command, "Import Test Asset"), ignored -> {}));
                context.subscriptions()
                        .add(context.commandPlacements()
                                .register(new EditorCommandPlacement(command, menu, "tools", 10)));
                context.subscriptions()
                        .add(context.commandPlacements()
                                .register(new EditorCommandPlacement(
                                        command, EditorCommandLocations.FILE_MENU, "extension", 90)));
                context.subscriptions()
                        .add(context.menus().register(new EditorMenuContribution(menu, "Test Tools", 90)));
            }
        };
    }

    private static EditorWorkbenchCommandSet.Actions emptyActions() {
        return new EditorWorkbenchCommandSet.Actions(() -> {}, () -> {}, () -> {}, () -> {}, () -> {}, () -> {});
    }

    private static EditorMenuSnapshot menu(List<EditorMenuSnapshot> menus, String title) {
        return menus.stream()
                .filter(menu -> menu.contribution().title().equals(title))
                .findFirst()
                .orElseThrow();
    }

    private static EditorMenuCommandSnapshot command(List<EditorMenuSnapshot> menus, CommandId command) {
        return menus.stream()
                .flatMap(menu -> menu.commands().stream())
                .filter(item -> item.contribution().id().equals(command))
                .findFirst()
                .orElseThrow();
    }

    private static EditorExtensionHost host() {
        return new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
    }
}
