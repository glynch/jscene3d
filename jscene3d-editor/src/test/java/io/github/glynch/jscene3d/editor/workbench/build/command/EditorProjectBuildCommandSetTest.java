/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.command;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommandKind;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.workbench.build.preference.InMemoryWorkspaceBuildPreferences;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuCommandSnapshot;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuSnapshot;
import io.github.glynch.jscene3d.editor.workbench.selection.EditorSelectionContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Verifies localized project-build commands, availability, and persisted toggle state. */
final class EditorProjectBuildCommandSetTest {
    @Test
    void contributesProjectMenuAndInvokesAvailableBuildCommands() {
        EditorExtensionHost host = host();
        InMemoryWorkspaceBuildPreferences preferences = new InMemoryWorkspaceBuildPreferences();
        AtomicInteger builds = new AtomicInteger();
        AtomicInteger rebuilds = new AtomicInteger();
        AtomicInteger cancellations = new AtomicInteger();
        AtomicInteger outputViews = new AtomicInteger();
        AtomicBoolean automatic = new AtomicBoolean();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();
        host.observeMenus(snapshots::add);
        EditorProjectBuildCommandSet commands = new EditorProjectBuildCommandSet(
                host,
                preferences,
                new EditorProjectBuildCommandSet.Actions(
                        builds::incrementAndGet,
                        rebuilds::incrementAndGet,
                        cancellations::incrementAndGet,
                        outputViews::incrementAndGet,
                        automatic::set));

        Path workspace = Path.of("projects/example");
        commands.openWorkspace(workspace);
        commands.update(new ProjectBuildCommandAvailability(true, false, true));

        assertThat(menu(snapshots.getLast()).commands())
                .extracting(item -> item.contribution().title())
                .containsExactly(
                        "Build Project", "Rebuild Project", "Cancel Build", "Show Build Output", "Build Automatically");
        assertThat(menu(snapshots.getLast()).commands())
                .extracting(EditorMenuCommandSnapshot::group)
                .containsExactly("build", "build", "build", "build", "configuration");
        assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                        .state()
                        .enabled())
                .isTrue();
        assertThat(command(snapshots.getLast(), EditorCommands.CANCEL_BUILD)
                        .state()
                        .enabled())
                .isFalse();
        assertThat(command(snapshots.getLast(), EditorCommands.TOGGLE_AUTOMATIC_BUILD)
                        .contribution()
                        .kind())
                .isEqualTo(EditorCommandKind.TOGGLE);
        assertThat(command(snapshots.getLast(), EditorCommands.TOGGLE_AUTOMATIC_BUILD)
                        .state()
                        .selected())
                .isTrue();

        host.execute(EditorCommands.BUILD_PROJECT);
        host.execute(EditorCommands.REBUILD_PROJECT);
        host.execute(EditorCommands.SHOW_BUILD_OUTPUT);
        host.execute(EditorCommands.TOGGLE_AUTOMATIC_BUILD);

        assertThat(builds).hasValue(1);
        assertThat(rebuilds).hasValue(1);
        assertThat(outputViews).hasValue(1);
        assertThat(automatic).isFalse();
        assertThat(preferences.automaticBuild(workspace)).isFalse();
        assertThat(command(snapshots.getLast(), EditorCommands.TOGGLE_AUTOMATIC_BUILD)
                        .state()
                        .selected())
                .isFalse();

        commands.update(new ProjectBuildCommandAvailability(true, true, true));
        host.execute(EditorCommands.CANCEL_BUILD);

        assertThat(cancellations).hasValue(1);
        assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                        .state()
                        .enabled())
                .isFalse();

        commands.close();
        host.close();
    }

    @Test
    void keepsCommandsDisabledUntilWorkspaceAndBuildCoordinatorAreAvailable() {
        EditorExtensionHost host = host();
        List<List<EditorMenuSnapshot>> snapshots = new ArrayList<>();
        host.observeMenus(snapshots::add);
        EditorProjectBuildCommandSet commands = new EditorProjectBuildCommandSet(
                host, new InMemoryWorkspaceBuildPreferences(), EditorProjectBuildCommandSet.Actions.unavailable());

        assertThat(menu(snapshots.getLast()).commands())
                .allSatisfy(command -> assertThat(command.state().enabled()).isFalse());

        commands.openWorkspace(Path.of("projects/example"));

        assertThat(command(snapshots.getLast(), EditorCommands.BUILD_PROJECT)
                        .state()
                        .enabled())
                .isFalse();
        assertThat(command(snapshots.getLast(), EditorCommands.TOGGLE_AUTOMATIC_BUILD)
                        .state()
                        .enabled())
                .isTrue();

        commands.closeWorkspace();

        assertThat(menu(snapshots.getLast()).commands())
                .allSatisfy(command -> assertThat(command.state().enabled()).isFalse());

        commands.close();
        host.close();
    }

    private static EditorMenuSnapshot menu(List<EditorMenuSnapshot> menus) {
        return menus.stream()
                .filter(menu -> menu.contribution().title().equals("Project"))
                .findFirst()
                .orElseThrow();
    }

    private static EditorMenuCommandSnapshot command(List<EditorMenuSnapshot> menus, CommandId command) {
        return menu(menus).commands().stream()
                .filter(item -> item.contribution().id().equals(command))
                .findFirst()
                .orElseThrow();
    }

    private static EditorExtensionHost host() {
        return new EditorExtensionHost(new EditorProjectContext(), new EditorSelectionContext());
    }
}
