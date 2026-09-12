/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.activity.EditorActivitySelection;
import io.github.glynch.jscene3d.editor.workbench.activity.JavaFxActivityBar;
import io.github.glynch.jscene3d.editor.workbench.command.EditorWorkbenchCommandSet;
import io.github.glynch.jscene3d.editor.workbench.command.EditorWorkbenchCommandSet.DocumentCommandState;
import io.github.glynch.jscene3d.editor.workbench.dialog.EditorWindowCloseGuard;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.layout.JavaFxLayoutCustomizer;
import io.github.glynch.jscene3d.editor.workbench.layout.JavaFxLayoutQuickAccess;
import io.github.glynch.jscene3d.editor.workbench.layout.JavaFxWorkbenchRegions;
import io.github.glynch.jscene3d.editor.workbench.menu.JavaFxMenuBar;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusBarPane;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxEditorArea;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxPanelPart;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxViewContainer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** Owns the editor's resizable JavaFX workspace and its visible document state. */
public final class EditorWorkspace extends BorderPane {
    private static final PseudoClass DIRTY_PSEUDO_CLASS = PseudoClass.getPseudoClass("dirty");
    private static final double TOP_CHROME_MARK_SIZE = 24.0;

    private final EditorSelections selections;
    private final EditorExtensionHost extensions;
    private final EditorWindowCloseGuard closeGuard;
    private final Label projectContext = new Label("No project");
    private final StringProperty previewTitle = new SimpleStringProperty("Empty Preview");
    private final BooleanProperty previewDirty = new SimpleBooleanProperty(false);
    private final JavaFxPanelPart bottomPanel;
    private final EditorWorkbenchLayout layout;
    private final JavaFxViewContainer primaryViewContainer;
    private final JavaFxViewContainer secondaryViewContainer;
    private final JavaFxActivityBar activityBar;
    private final EditorActivitySelection activitySelection;
    private final EditorRegistration activitySelectionRegistration;
    private final JavaFxLayoutCustomizer layoutCustomizer;
    private final JavaFxLayoutQuickAccess layoutQuickAccess;
    private final JavaFxEditorArea editorArea;
    private final JavaFxWorkbenchRegions regions;
    private final EditorStatusBarPane statusBar;
    private final EditorWorkbenchCommandSet commandSet;
    private final JavaFxMenuBar menuBar;

    private EditorRegistration documentRegistration = () -> {};
    private EditorRegistration dirtyRegistration = () -> {};
    private @Nullable EditorProjectSession document;

    /** Creates the shell around an existing viewport and the real open-project command. */
    EditorWorkspace(
            GLCanvas viewportCanvas,
            Runnable openProject,
            Runnable requestClose,
            EditorSelections selections,
            EditorExtensionHost extensions,
            EditorBuildInfo buildInfo) {
        this.selections = Objects.requireNonNull(selections, "selections");
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        closeGuard = new EditorWindowCloseGuard(extensions::showDialog);
        JavaFxIconRenderer icons = JavaFxIconRenderer.builtIn();
        layout = new EditorWorkbenchLayout(extensions);
        activitySelection = new EditorActivitySelection(extensions, layout);
        primaryViewContainer = new JavaFxViewContainer(extensions, layout, EditorViewContainers.PRIMARY_SIDEBAR, icons);
        activitySelectionRegistration = activitySelection.observe(primaryViewContainer::showActivity);
        VBox hierarchyPanel = primaryViewContainer.node();
        hierarchyPanel
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_HIERARCHY_PANEL);
        secondaryViewContainer =
                new JavaFxViewContainer(extensions, layout, EditorViewContainers.SECONDARY_SIDEBAR, icons);
        VBox inspectorPanel = secondaryViewContainer.node();
        inspectorPanel
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_INSPECTOR_PANEL);
        editorArea = new JavaFxEditorArea(
                extensions, layout, icons, previewTitle, previewDirty, createViewportContent(viewportCanvas));
        bottomPanel = new JavaFxPanelPart(
                extensions,
                layout,
                EditorViewContainers.BOTTOM_PANEL,
                icons,
                EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT,
                EditorWorkspaceLayout.PREFERRED_BOTTOM_HEIGHT,
                EditorWorkspaceLayout::verticalForBottomHeight);
        statusBar = new EditorStatusBarPane(extensions, icons);
        activityBar = new JavaFxActivityBar(extensions, activitySelection, icons);
        regions = new JavaFxWorkbenchRegions(
                layout,
                activityBar.node(),
                hierarchyPanel,
                editorArea.node(),
                bottomPanel,
                inspectorPanel,
                statusBar.node());
        layoutCustomizer = new JavaFxLayoutCustomizer(layout, icons, extensions::showView);
        layoutQuickAccess = new JavaFxLayoutQuickAccess(layout, icons, layoutCustomizer.button());
        commandSet = new EditorWorkbenchCommandSet(
                extensions,
                new EditorWorkbenchCommandSet.Actions(
                        openProject,
                        this::showProjectSettings,
                        this::saveDocument,
                        this::undo,
                        this::redo,
                        requestClose),
                Objects.requireNonNull(buildInfo, "buildInfo").aboutText());
        menuBar = new JavaFxMenuBar(extensions);
        setTop(createTopChrome(menuBar.node(), layoutQuickAccess.node()));
        setCenter(regions.node());
        getStyleClass().add(EditorStyleClasses.EDITOR_SHELL);
    }

    /** Applies bounded initial divider positions after the stage has completed its first layout. */
    void applyInitialDividerPositions() {
        regions.applyInitialDividerPositions();
    }

    /** Updates the viewport portion of the status bar. */
    void setViewportStatus(String text) {
        statusBar.showViewportStatus(text);
    }

    /**
     * Shows that a project directory is being opened without claiming it has loaded.
     *
     * @param directory project directory being opened
     */
    public void beginOpening(Path directory) {
        clearSelection();
        Path normalized = directory.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        String candidateName = fileName == null ? normalized.toString() : fileName.toString();
        projectContext.setText(candidateName);
        projectContext.setAccessibleText(candidateName);
        projectContext.setTooltip(new Tooltip(normalized.toString()));
        setProjectStatus("Opening " + candidateName + "…");
    }

    /**
     * Replaces the visible hierarchy, Project content, and preview context atomically.
     *
     * @param session completely loaded editor project session
     */
    public void showProject(EditorProjectSession session) {
        clearSelection();
        documentRegistration.close();
        dirtyRegistration.close();
        editorArea.closeProjectSettings();
        document = Objects.requireNonNull(session, "session");
        documentRegistration = session.onDidChangeHierarchy().subscribe(ignored -> updateDocumentCommands());
        dirtyRegistration = session.workingCopies().onDidChangeDirty().subscribe(ignored -> updateDocumentCommands());
        previewTitle.set(session.hierarchy().label() + " Preview");
        updateDocumentCommands();
    }

    /** Clears project-owned views after an unsuccessful open. */
    public void clearProject() {
        clearSelection();
        documentRegistration.close();
        dirtyRegistration.close();
        editorArea.closeProjectSettings();
        documentRegistration = () -> {};
        dirtyRegistration = () -> {};
        document = null;
        projectContext.setText("No project");
        projectContext.setAccessibleText("No project");
        projectContext.setTooltip(null);
        previewTitle.set("Empty Preview");
        previewDirty.set(false);
        updateDocumentCommands();
    }

    /**
     * Updates the concise project portion of the status bar.
     *
     * @param text project status text
     */
    public void setProjectStatus(String text) {
        statusBar.showProjectStatus(text);
    }

    /**
     * Shows an extension message without exposing JavaFX through the extension interface.
     *
     * @param message extension message to show
     */
    public void showMessage(EditorMessage message) {
        statusBar.showMessage(message);
    }

    /** Confirms or saves dirty resources before an orderly window close. */
    boolean prepareToClose() {
        EditorProjectSession current = document;
        if (current == null || !current.isDirty()) {
            return true;
        }
        return closeGuard.confirmClose(
                current.project().identity().name(),
                current.workingCopies().dirtyCount(),
                () -> document == current && trySaveDocument());
    }

    /** Releases workbench adapters before the extension host is closed. */
    void close() {
        documentRegistration.close();
        dirtyRegistration.close();
        menuBar.close();
        commandSet.close();
        layoutQuickAccess.close();
        layoutCustomizer.close();
        regions.close();
        activityBar.close();
        activitySelectionRegistration.close();
        activitySelection.close();
        editorArea.close();
        statusBar.close();
        secondaryViewContainer.close();
        bottomPanel.close();
        primaryViewContainer.close();
        layout.close();
    }

    /** Creates compact product, menu, project-context, and command chrome. */
    private HBox createTopChrome(MenuBar menus, HBox layoutActions) {
        EditorBrandMark productMark = new EditorBrandMark(TOP_CHROME_MARK_SIZE);
        productMark.getStyleClass().add(EditorStyleClasses.EDITOR_PRODUCT_MARK);
        Label productKind = new Label("EDITOR");
        productKind.getStyleClass().add(EditorStyleClasses.EDITOR_PRODUCT_KIND);

        updateDocumentCommands();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        projectContext.setMaxWidth(300.0);
        projectContext.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        projectContext.getStyleClass().add(EditorStyleClasses.EDITOR_PROJECT_CONTEXT);
        Button openButton = new Button("Open Project…");
        openButton.setOnAction(ignored -> extensions.execute(EditorCommands.OPEN_PROJECT));
        openButton.getStyleClass().add(EditorStyleClasses.EDITOR_OPEN_PROJECT_BUTTON);

        HBox chrome = new HBox(8.0, productMark, productKind, menus, spacer, projectContext, layoutActions, openButton);
        chrome.setAlignment(Pos.CENTER_LEFT);
        chrome.getStyleClass().add(EditorStyleClasses.EDITOR_TOP);
        return chrome;
    }

    private void saveDocument() {
        trySaveDocument();
    }

    private boolean trySaveDocument() {
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

    private void showProjectSettings() {
        EditorProjectSession current = document;
        if (current != null) {
            editorArea.showProjectSettings(current, this::showMessage);
        }
    }

    private void undo() {
        EditorProjectSession current = document;
        if (current != null) {
            current.undo();
        }
    }

    private void redo() {
        EditorProjectSession current = document;
        if (current != null) {
            current.redo();
        }
    }

    private void updateDocumentCommands() {
        EditorProjectSession current = document;
        boolean dirty = current != null && current.isDirty();
        boolean worldDirty =
                current != null && current.startupWorldWorkingCopy().isDirty();
        previewDirty.set(worldDirty);
        commandSet.update(new DocumentCommandState(
                current != null, dirty, current != null && current.canUndo(), current != null && current.canRedo()));
        if (current != null) {
            String projectName = current.project().identity().name();
            projectContext.setText(dirty ? projectName + '*' : projectName);
            projectContext.setAccessibleText(dirty ? projectName + ", modified" : projectName);
            projectContext.pseudoClassStateChanged(DIRTY_PSEUDO_CLASS, dirty);
            if (dirty) {
                setProjectStatus(projectName + " · modified");
            } else {
                setProjectStatus(projectName);
            }
        } else {
            projectContext.pseudoClassStateChanged(DIRTY_PSEUDO_CLASS, false);
        }
    }

    /** Clears UI and shared selection state before project-owned values are replaced. */
    private void clearSelection() {
        selections.clear();
    }

    /** Creates the scene-preview content hosted by the central editor area. */
    private static StackPane createViewportContent(GLCanvas viewportCanvas) {
        StackPane viewport = new StackPane(viewportCanvas);
        viewport.setMinHeight(EditorWorkspaceLayout.MINIMUM_PREVIEW_HEIGHT);
        viewport.getStyleClass().add(EditorStyleClasses.EDITOR_VIEWPORT);
        return viewport;
    }
}
