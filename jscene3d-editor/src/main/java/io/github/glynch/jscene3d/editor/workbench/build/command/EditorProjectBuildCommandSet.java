/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.command;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommand;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandKind;
import io.github.glynch.jscene3d.editor.command.EditorCommandLocations;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistration;
import io.github.glynch.jscene3d.editor.command.EditorCommandState;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import io.github.glynch.jscene3d.editor.workbench.build.preference.WorkspaceBuildPreferences;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/** Owns Project-menu build commands and their shared presentation state. */
public final class EditorProjectBuildCommandSet implements AutoCloseable {
    private static final String MESSAGE_BUNDLE = "io.github.glynch.jscene3d.editor.workbench.build.messages";

    private final List<EditorRegistration> registrations = new ArrayList<>();
    private final WorkspaceBuildPreferences preferences;
    private final Actions actions;
    private final MessageSource messages;
    private final Locale locale;
    private final EditorCommandRegistration build;
    private final EditorCommandRegistration rebuild;
    private final EditorCommandRegistration cancel;
    private final EditorCommandRegistration showOutput;
    private final EditorCommandRegistration automaticBuild;

    private @Nullable Path workspaceRoot;
    private ProjectBuildCommandAvailability availability = ProjectBuildCommandAvailability.UNAVAILABLE;
    private boolean automaticBuildSelected;

    /**
     * Creates the localized Project-menu build command set.
     *
     * @param extensions workbench command and menu host
     * @param preferences per-workspace automatic-build preferences
     * @param actions behavior supplied by the project build coordinator
     */
    public EditorProjectBuildCommandSet(
            EditorExtensionHost extensions, WorkspaceBuildPreferences preferences, Actions actions) {
        this(
                extensions,
                preferences,
                actions,
                new ResourceBundleMessageSource(EditorProjectBuildCommandSet.class.getModule(), MESSAGE_BUNDLE),
                Locale.getDefault(Locale.Category.DISPLAY));
    }

    EditorProjectBuildCommandSet(
            EditorExtensionHost extensions,
            WorkspaceBuildPreferences preferences,
            Actions actions,
            MessageSource messages,
            Locale locale) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.actions = Objects.requireNonNull(actions, "actions");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.locale = Objects.requireNonNull(locale, "locale");

        registrations.add(host.registerMenu(new EditorMenuContribution(
                EditorCommandLocations.PROJECT_MENU, message("project.menu", "Project"), 40)));
        build = register(
                host,
                EditorCommands.BUILD_PROJECT,
                message("project.build", "Build Project"),
                ignored -> actions.build().run());
        rebuild = register(
                host,
                EditorCommands.REBUILD_PROJECT,
                message("project.rebuild", "Rebuild Project"),
                ignored -> actions.rebuild().run());
        cancel = register(
                host,
                EditorCommands.CANCEL_BUILD,
                message("project.build.cancel", "Cancel Build"),
                ignored -> actions.cancel().run());
        showOutput = register(
                host,
                EditorCommands.SHOW_BUILD_OUTPUT,
                message("project.build.output", "Show Build Output"),
                ignored -> actions.showOutput().run());
        automaticBuild = register(
                host,
                new EditorCommandContribution(
                        EditorCommands.TOGGLE_AUTOMATIC_BUILD,
                        message("project.build.automatic", "Build Automatically"),
                        EditorCommandKind.TOGGLE),
                ignored -> toggleAutomaticBuild());
        updateRegistrations();
        registerPlacements(host);
    }

    /**
     * Selects a workspace and restores its automatic-build preference.
     *
     * @param projectRoot workspace root to select
     */
    public void openWorkspace(Path projectRoot) {
        workspaceRoot = normalize(projectRoot);
        availability = ProjectBuildCommandAvailability.UNAVAILABLE;
        automaticBuildSelected = preferences.automaticBuild(workspaceRoot);
        updateRegistrations();
    }

    /** Clears workspace-specific command state after the project closes. */
    public void closeWorkspace() {
        workspaceRoot = null;
        availability = ProjectBuildCommandAvailability.UNAVAILABLE;
        automaticBuildSelected = false;
        updateRegistrations();
    }

    /**
     * Replaces build capabilities supplied by the project build coordinator.
     *
     * @param replacement latest command availability
     */
    public void update(ProjectBuildCommandAvailability replacement) {
        availability = Objects.requireNonNull(replacement, "availability");
        updateRegistrations();
    }

    @Override
    public void close() {
        List.copyOf(registrations).reversed().forEach(EditorRegistration::close);
        registrations.clear();
        workspaceRoot = null;
    }

    private void toggleAutomaticBuild() {
        Path currentWorkspace = workspaceRoot;
        if (currentWorkspace == null) {
            throw new IllegalStateException("automatic build requires an open workspace");
        }
        automaticBuildSelected = !automaticBuildSelected;
        preferences.saveAutomaticBuild(currentWorkspace, automaticBuildSelected);
        updateRegistrations();
        actions.automaticBuildChanged().accept(automaticBuildSelected);
    }

    private void updateRegistrations() {
        boolean projectOpen = workspaceRoot != null;
        boolean building = availability.building();
        boolean canBuild = projectOpen && availability.buildAvailable() && !building;
        build.update(enabled(canBuild));
        rebuild.update(enabled(canBuild));
        cancel.update(enabled(projectOpen && building));
        showOutput.update(enabled(projectOpen && availability.outputAvailable()));
        automaticBuild.update(new EditorCommandState(projectOpen, automaticBuildSelected));
    }

    private void registerPlacements(EditorExtensionHost host) {
        place(host, EditorCommands.BUILD_PROJECT, "build", 10);
        place(host, EditorCommands.REBUILD_PROJECT, "build", 20);
        place(host, EditorCommands.CANCEL_BUILD, "build", 30);
        place(host, EditorCommands.SHOW_BUILD_OUTPUT, "build", 40);
        place(host, EditorCommands.TOGGLE_AUTOMATIC_BUILD, "configuration", 50);
    }

    private EditorCommandRegistration register(
            EditorExtensionHost host, CommandId id, String title, EditorCommand command) {
        return register(host, new EditorCommandContribution(id, title), command);
    }

    private EditorCommandRegistration register(
            EditorExtensionHost host, EditorCommandContribution contribution, EditorCommand command) {
        EditorCommandRegistration registration = host.registerCommand(contribution, command);
        registrations.add(registration);
        return registration;
    }

    private void place(EditorExtensionHost host, CommandId command, String group, int order) {
        registrations.add(host.registerCommandPlacement(
                new EditorCommandPlacement(command, EditorCommandLocations.PROJECT_MENU, group, order)));
    }

    private String message(String code, String fallback) {
        return messages.getMessage(code, fallback, locale);
    }

    private static Path normalize(Path projectRoot) {
        return Objects.requireNonNull(projectRoot, "projectRoot")
                .toAbsolutePath()
                .normalize();
    }

    private static EditorCommandState enabled(boolean enabled) {
        return enabled ? EditorCommandState.ENABLED_STATE : EditorCommandState.DISABLED_STATE;
    }

    /**
     * Build behavior supplied by the project build coordinator.
     *
     * @param build requests an incremental development build
     * @param rebuild requests a clean development build
     * @param cancel cancels the active build
     * @param showOutput opens the current project's build output
     * @param automaticBuildChanged accepts persisted automatic-build changes
     */
    public record Actions(
            Runnable build,
            Runnable rebuild,
            Runnable cancel,
            Runnable showOutput,
            Consumer<Boolean> automaticBuildChanged) {
        /** Validates every required project-build action. */
        public Actions {
            Objects.requireNonNull(build, "build");
            Objects.requireNonNull(rebuild, "rebuild");
            Objects.requireNonNull(cancel, "cancel");
            Objects.requireNonNull(showOutput, "showOutput");
            Objects.requireNonNull(automaticBuildChanged, "automaticBuildChanged");
        }

        /**
         * Returns inert actions used before the project build coordinator is installed.
         *
         * @return inert build actions
         */
        public static Actions unavailable() {
            return new Actions(() -> {}, () -> {}, () -> {}, () -> {}, ignored -> {});
        }
    }
}
