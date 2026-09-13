/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench;

import io.github.glynch.jscene3d.editor.application.EditorBuildInfo;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.project.session.EditorProjectSession;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.command.EditorWorkbenchCommandSet;
import io.github.glynch.jscene3d.editor.workbench.command.EditorWorkbenchCommandSet.DocumentCommandState;
import io.github.glynch.jscene3d.editor.workbench.dialog.EditorWindowCloseGuard;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusBarPane;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxEditorArea;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxFileEditors;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxPreviewEditor;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxSettingsEditor;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxWelcomeEditor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.jspecify.annotations.Nullable;

/** Coordinates project and file document state without owning the editor-tab surface. */
final class EditorWorkspaceDocument implements AutoCloseable {
    record Context(
            EditorExtensionHost extensions,
            EditorSelections selections,
            JavaFxEditorArea editorArea,
            JavaFxIconRenderer icons,
            Node previewContent,
            Label projectContext,
            EditorStatusBarPane statusBar) {}

    record Actions(Runnable openProject, Runnable requestClose, EditorBuildInfo buildInfo) {}

    private static final PseudoClass DIRTY_PSEUDO_CLASS = PseudoClass.getPseudoClass("dirty");

    private final EditorSelections selections;
    private final EditorWindowCloseGuard closeGuard;
    private final Label projectContext;
    private final StringProperty previewTitle = new SimpleStringProperty("Empty Preview");
    private final BooleanProperty previewDirty = new SimpleBooleanProperty(false);
    private final EditorStatusBarPane statusBar;
    private final JavaFxPreviewEditor previewEditor;
    private final JavaFxWelcomeEditor welcomeEditor;
    private final JavaFxSettingsEditor settingsEditor;
    private final JavaFxFileEditors fileEditors;
    private final EditorWorkbenchCommandSet commandSet;
    private final EditorRegistration selectionRegistration;

    private EditorRegistration documentRegistration = () -> {};
    private EditorRegistration dirtyRegistration = () -> {};
    private @Nullable EditorProjectSession document;

    EditorWorkspaceDocument(Context context, Actions actions) {
        Context workspace = Objects.requireNonNull(context, "context");
        Actions commands = Objects.requireNonNull(actions, "actions");
        EditorExtensionHost host = Objects.requireNonNull(workspace.extensions(), "extensions");
        this.selections = Objects.requireNonNull(workspace.selections(), "selections");
        this.projectContext = Objects.requireNonNull(workspace.projectContext(), "projectContext");
        this.statusBar = Objects.requireNonNull(workspace.statusBar(), "statusBar");
        closeGuard = new EditorWindowCloseGuard(host::showDialog);
        JavaFxEditorArea area = Objects.requireNonNull(workspace.editorArea(), "editorArea");
        JavaFxIconRenderer icons = Objects.requireNonNull(workspace.icons(), "icons");
        previewEditor = new JavaFxPreviewEditor(area, previewTitle, previewDirty, workspace.previewContent());
        welcomeEditor = new JavaFxWelcomeEditor(area, icons, () -> host.execute(EditorCommands.OPEN_PROJECT));
        settingsEditor = new JavaFxSettingsEditor(area, host);
        fileEditors = new JavaFxFileEditors(area, host, icons, this::updateCommands);
        commandSet = new EditorWorkbenchCommandSet(
                host,
                new EditorWorkbenchCommandSet.Actions(
                        commands.openProject(),
                        this::showSettings,
                        host.colorThemes()::toggleColorScheme,
                        this::saveDocument,
                        this::undo,
                        this::redo,
                        commands.requestClose()),
                Objects.requireNonNull(commands.buildInfo(), "buildInfo").aboutText());
        selectionRegistration = area.observeSelectionChanged(this::updateCommands);
        previewEditor.show();
    }

    void showWelcome() {
        previewEditor.hide();
        settingsEditor.close();
        welcomeEditor.show();
    }

    void beginOpening(Path directory) {
        selections.clear();
        previewEditor.show();
        Path normalized = directory.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        String candidateName = fileName == null ? normalized.toString() : fileName.toString();
        projectContext.setText(candidateName);
        projectContext.setAccessibleText(candidateName);
        projectContext.setTooltip(new Tooltip(normalized.toString()));
        setProjectStatus("Opening " + candidateName + "…");
    }

    void showProject(EditorProjectSession session) {
        selections.clear();
        documentRegistration.close();
        dirtyRegistration.close();
        settingsEditor.close();
        document = Objects.requireNonNull(session, "session");
        documentRegistration = session.onDidChangeHierarchy().subscribe(ignored -> updateCommands());
        dirtyRegistration = session.workingCopies().onDidChangeDirty().subscribe(ignored -> updateCommands());
        previewTitle.set(session.hierarchy().label() + " Preview");
        previewEditor.show();
        updateCommands();
    }

    void clearProject() {
        selections.clear();
        documentRegistration.close();
        dirtyRegistration.close();
        settingsEditor.close();
        previewEditor.hide();
        welcomeEditor.show();
        documentRegistration = () -> {};
        dirtyRegistration = () -> {};
        document = null;
        projectContext.setText("No project");
        projectContext.setAccessibleText("No project");
        projectContext.setTooltip(null);
        previewTitle.set("Empty Preview");
        previewDirty.set(false);
        updateCommands();
    }

    void setProjectStatus(String text) {
        statusBar.showProjectStatus(text);
    }

    void showMessage(EditorMessage message) {
        statusBar.showMessage(message);
    }

    boolean prepareToClose() {
        EditorProjectSession current = document;
        int projectDirtyCount = current == null ? 0 : current.workingCopies().dirtyCount();
        int dirtyCount = projectDirtyCount + fileEditors.dirtyCount();
        if (dirtyCount == 0) {
            return true;
        }
        String workspaceName =
                current == null ? "open files" : current.project().identity().name();
        return closeGuard.confirmClose(workspaceName, dirtyCount, () -> document == current && trySaveAllResources());
    }

    private void saveDocument() {
        if (fileEditors.hasSelectedFile()) {
            if (fileEditors.saveSelectedFile()) {
                setProjectStatus("File saved");
            }
        } else {
            trySaveProject();
        }
    }

    private boolean trySaveProject() {
        EditorProjectSession current = document;
        if (current == null) {
            return true;
        }
        try {
            current.save();
            setProjectStatus(current.project().identity().name() + " · saved");
            return true;
        } catch (IOException exception) {
            setProjectStatus("Unable to save " + current.project().identity().name());
            showMessage(new EditorMessage(
                    EditorMessageSeverity.ERROR,
                    "Unable to save the project: "
                            + Objects.requireNonNullElse(exception.getMessage(), exception.toString())));
            return false;
        }
    }

    private void showSettings() {
        settingsEditor.show(document, this::showMessage);
    }

    private void undo() {
        if (fileEditors.hasSelectedFile()) {
            fileEditors.undoSelectedFile();
        } else if (document != null) {
            document.undo();
        }
    }

    private void redo() {
        if (fileEditors.hasSelectedFile()) {
            fileEditors.redoSelectedFile();
        } else if (document != null) {
            document.redo();
        }
    }

    private void updateCommands() {
        EditorProjectSession current = document;
        boolean projectDirty = current != null && current.isDirty();
        boolean fileSelected = fileEditors.hasSelectedFile();
        boolean dirty = fileSelected ? fileEditors.isSelectedFileDirty() : projectDirty;
        boolean worldDirty =
                current != null && current.startupWorldWorkingCopy().isDirty();
        previewDirty.set(worldDirty);
        commandSet.update(new DocumentCommandState(
                current != null,
                dirty,
                fileSelected || current != null && current.canUndo(),
                fileSelected || current != null && current.canRedo()));
        updateProjectPresentation(current, projectDirty);
    }

    private void updateProjectPresentation(@Nullable EditorProjectSession current, boolean dirty) {
        if (current == null) {
            projectContext.pseudoClassStateChanged(DIRTY_PSEUDO_CLASS, false);
            return;
        }
        String projectName = current.project().identity().name();
        projectContext.setText(dirty ? projectName + '*' : projectName);
        projectContext.setAccessibleText(dirty ? projectName + ", modified" : projectName);
        projectContext.pseudoClassStateChanged(DIRTY_PSEUDO_CLASS, dirty);
        setProjectStatus(dirty ? projectName + " · modified" : projectName);
    }

    private boolean trySaveAllResources() {
        return trySaveProject() && fileEditors.saveAll();
    }

    @Override
    public void close() {
        documentRegistration.close();
        dirtyRegistration.close();
        selectionRegistration.close();
        fileEditors.close();
        settingsEditor.close();
        welcomeEditor.close();
        previewEditor.close();
        commandSet.close();
    }
}
