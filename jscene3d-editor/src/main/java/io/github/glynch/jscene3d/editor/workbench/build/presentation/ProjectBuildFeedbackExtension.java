/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.build.presentation;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.output.EditorOutputChannel;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusBarAlignment;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIconId;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildCompletion;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildPhase;
import io.github.glynch.jscene3d.editor.workbench.build.coordination.ProjectBuildSnapshot;
import io.github.glynch.jscene3d.i18n.MessageSource;
import io.github.glynch.jscene3d.i18n.resourcebundle.ResourceBundleMessageSource;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Built-in extension presenting project build state, output, and diagnostics. */
public final class ProjectBuildFeedbackExtension implements EditorExtension {
    /** Stable identity of the Build output view. */
    public static final ViewId OUTPUT_VIEW_ID = new ViewId("io.github.glynch.jscene3d.editor.project-build-output");

    private static final String EXTENSION_ID = "io.github.glynch.jscene3d.editor.builtin.project-build-feedback";
    private static final String MESSAGE_BUNDLE = "io.github.glynch.jscene3d.editor.workbench.build.messages";
    private static final StatusItemId STATUS_ID = new StatusItemId(EXTENSION_ID + ".status");
    private static final DiagnosticCollectionId DIAGNOSTICS_ID = new DiagnosticCollectionId(EXTENSION_ID);

    private final MessageSource messages;
    private final Locale locale;
    private final EditorOutputChannel output;

    private @Nullable EditorStatusItem status;
    private @Nullable ProjectBuildDiagnosticPublisher diagnostics;
    private @Nullable Path projectRoot;
    private boolean outputAvailable;

    /** Creates build feedback localized for the current display locale. */
    public ProjectBuildFeedbackExtension() {
        this(
                new ResourceBundleMessageSource(ProjectBuildFeedbackExtension.class.getModule(), MESSAGE_BUNDLE),
                Locale.getDefault(Locale.Category.DISPLAY));
    }

    ProjectBuildFeedbackExtension(MessageSource messages, Locale locale) {
        this.messages = Objects.requireNonNull(messages, "messages");
        this.locale = Objects.requireNonNull(locale, "locale");
        output = new EditorOutputChannel(OUTPUT_VIEW_ID, message("project.build.output.title", "Build"));
    }

    @Override
    public String id() {
        return EXTENSION_ID;
    }

    @Override
    public EditorExtensionDescriptor descriptor() {
        return new EditorExtensionDescriptor(
                id(),
                message("project.build.feedback.name", "Project Build Feedback"),
                message(
                        "project.build.feedback.description",
                        "Presents project build status, output, and diagnostics."),
                "JScene3D",
                Optional.empty(),
                true);
    }

    @Override
    public void activate(EditorExtensionContext context) {
        EditorExtensionContext editor = Objects.requireNonNull(context, "context");
        editor.subscriptions().add(output);
        editor.subscriptions()
                .add(editor.views()
                        .register(new EditorViewContribution(
                                output,
                                EditorViewContainers.BOTTOM_PANEL,
                                30,
                                EditorContextCondition.isTrue(EditorContextKeys.PROJECT_OPEN))));
        status = editor.subscriptions()
                .add(editor.statusBar()
                        .create(new EditorStatusItemContribution(STATUS_ID, StatusBarAlignment.LEFT, 90)));
        EditorDiagnosticCollection collection =
                editor.subscriptions().add(editor.diagnostics().createCollection(DIAGNOSTICS_ID));
        diagnostics = new ProjectBuildDiagnosticPublisher(collection);
    }

    /**
     * Starts a fresh feedback lifetime for one open workspace.
     *
     * @param root absolute workspace root
     */
    public void openWorkspace(Path root) {
        projectRoot = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        outputAvailable = false;
        output.clear();
        requireDiagnostics().clear();
        requireStatus().update(status(ProjectBuildPhase.UNKNOWN));
    }

    /** Hides build feedback and removes project-owned output and diagnostics. */
    public void closeWorkspace() {
        projectRoot = null;
        outputAvailable = false;
        output.clear();
        requireDiagnostics().clear();
        requireStatus().update(hiddenStatus());
    }

    /**
     * Presents the newest project build lifecycle snapshot.
     *
     * @param snapshot current build lifecycle snapshot
     */
    public void show(ProjectBuildSnapshot snapshot) {
        if (projectRoot != null) {
            requireStatus()
                    .update(status(Objects.requireNonNull(snapshot, "snapshot").phase()));
        }
    }

    /**
     * Replaces output and diagnostics with one terminal build completion.
     *
     * @param completion terminal build completion
     * @param currentSavedRevision newest saved project revision
     */
    public void show(ProjectBuildCompletion completion, long currentSavedRevision) {
        Path root = projectRoot;
        if (root == null) {
            return;
        }
        ProjectBuildCompletion event = Objects.requireNonNull(completion, "completion");
        output.replace(ProjectBuildOutputFormatter.format(event));
        outputAvailable = true;
        requireDiagnostics().publish(root, event, currentSavedRevision);
    }

    /**
     * Returns whether this project session has terminal build output.
     *
     * @return whether terminal output is available
     */
    public boolean outputAvailable() {
        return outputAvailable;
    }

    EditorOutputChannel output() {
        return output;
    }

    private EditorStatusItemState status(ProjectBuildPhase phase) {
        return switch (phase) {
            case UNKNOWN, STALE ->
                visibleStatus(
                        "project.build.status.required",
                        "Build required",
                        EditorIcons.WARNING,
                        EditorCommands.BUILD_PROJECT);
            case QUEUED -> visibleStatus("project.build.status.queued", "Build queued", EditorIcons.INFORMATION, null);
            case BUILDING -> visibleStatus("project.build.status.building", "Building", EditorIcons.INFORMATION, null);
            case CURRENT ->
                visibleStatus(
                        "project.build.status.succeeded",
                        "Build succeeded",
                        EditorIcons.INFORMATION,
                        outputAvailable ? EditorCommands.SHOW_BUILD_OUTPUT : null);
            case FAILED ->
                visibleStatus(
                        "project.build.status.failed",
                        "Build failed",
                        EditorIcons.ERROR,
                        EditorCommands.OPEN_DIAGNOSTICS);
        };
    }

    private EditorStatusItemState visibleStatus(
            String code, String fallback, EditorIconId icon, @Nullable CommandId command) {
        String text = message(code, fallback);
        return new EditorStatusItemState(
                text, Optional.of(new EditorIcon(icon, text)), Optional.of(text), Optional.ofNullable(command), true);
    }

    private static EditorStatusItemState hiddenStatus() {
        return new EditorStatusItemState("Build", Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    private EditorStatusItem requireStatus() {
        EditorStatusItem current = status;
        if (current == null) {
            throw new IllegalStateException("project build feedback extension has not been activated");
        }
        return current;
    }

    private ProjectBuildDiagnosticPublisher requireDiagnostics() {
        ProjectBuildDiagnosticPublisher current = diagnostics;
        if (current == null) {
            throw new IllegalStateException("project build feedback extension has not been activated");
        }
        return current;
    }

    private String message(String code, String fallback) {
        return messages.getMessage(code, fallback, locale);
    }
}
