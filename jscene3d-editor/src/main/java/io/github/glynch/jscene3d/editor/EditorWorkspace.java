/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusBarPane;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxPanelPart;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxViewContainer;
import java.nio.file.Path;
import java.util.Objects;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Owns the editor's resizable JavaFX workspace and its visible read-only state. */
public final class EditorWorkspace extends BorderPane {
    private final EditorSelections selections;
    private final Label projectContext = new Label("No project");
    private final Label previewTitle = new Label("Empty Preview");
    private final SplitPane workspaceSplit;
    private final SplitPane upperWorkspaceSplit;
    private final SplitPane leftWorkspaceSplit;
    private final JavaFxPanelPart bottomPanel;
    private final JavaFxViewContainer primaryViewContainer;
    private final JavaFxViewContainer secondaryViewContainer;
    private final EditorStatusBarPane statusBar;

    /** Creates the shell around an existing viewport and the real open-project command. */
    EditorWorkspace(
            GLCanvas viewportCanvas,
            Runnable openProject,
            EditorSelections selections,
            EditorExtensionHost extensions) {
        this.selections = Objects.requireNonNull(selections, "selections");
        setTop(createTopChrome(openProject));

        JavaFxIconRenderer icons = JavaFxIconRenderer.builtIn();
        primaryViewContainer = new JavaFxViewContainer(extensions, EditorViewContainers.PRIMARY_SIDEBAR, icons);
        VBox hierarchyPanel = primaryViewContainer.node();
        hierarchyPanel.setMinWidth(EditorWorkspaceLayout.MINIMUM_HIERARCHY_WIDTH);
        hierarchyPanel.setPrefWidth(EditorWorkspaceLayout.PREFERRED_HIERARCHY_WIDTH);
        hierarchyPanel
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_HIERARCHY_PANEL);
        secondaryViewContainer = new JavaFxViewContainer(extensions, EditorViewContainers.SECONDARY_SIDEBAR, icons);
        VBox inspectorPanel = secondaryViewContainer.node();
        inspectorPanel.setMinWidth(EditorWorkspaceLayout.MINIMUM_INSPECTOR_WIDTH);
        inspectorPanel.setPrefWidth(EditorWorkspaceLayout.PREFERRED_INSPECTOR_WIDTH);
        inspectorPanel
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_INSPECTOR_PANEL);
        VBox previewPanel = createViewportPane(viewportCanvas);
        upperWorkspaceSplit = createUpperWorkspaceSplit(hierarchyPanel, previewPanel);
        bottomPanel = new JavaFxPanelPart(
                extensions,
                EditorViewContainers.BOTTOM_PANEL,
                icons,
                EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT,
                EditorWorkspaceLayout.PREFERRED_BOTTOM_HEIGHT,
                EditorWorkspaceLayout::verticalForBottomHeight);
        leftWorkspaceSplit = createLeftWorkspaceSplit(upperWorkspaceSplit, bottomPanel);
        workspaceSplit = new SplitPane(leftWorkspaceSplit, inspectorPanel);
        workspaceSplit.setOrientation(Orientation.HORIZONTAL);
        workspaceSplit.getStyleClass().add(EditorStyleClasses.EDITOR_WORKSPACE_SPLIT);
        SplitPane.setResizableWithParent(inspectorPanel, false);
        setCenter(workspaceSplit);
        statusBar = new EditorStatusBarPane(extensions, icons);
        setBottom(statusBar.node());
        getStyleClass().add(EditorStyleClasses.EDITOR_SHELL);
    }

    /** Applies bounded initial divider positions after the stage has completed its first layout. */
    void applyInitialDividerPositions() {
        EditorWorkspaceLayout.HorizontalDividers horizontal =
                EditorWorkspaceLayout.horizontal(workspaceSplit.getWidth());
        workspaceSplit.setDividerPositions(horizontal.inspectorStart());
        upperWorkspaceSplit.setDividerPositions(horizontal.hierarchyEnd());
        leftWorkspaceSplit.setDividerPositions(EditorWorkspaceLayout.vertical(leftWorkspaceSplit.getHeight()));
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
        String projectName = session.project().identity().name();
        projectContext.setText(projectName);
        previewTitle.setText(session.hierarchy().label() + " Preview");
    }

    /** Clears project-owned views after an unsuccessful open. */
    public void clearProject() {
        clearSelection();
        projectContext.setText("No project");
        projectContext.setTooltip(null);
        previewTitle.setText("Empty Preview");
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

    /** Releases workbench adapters before the extension host is closed. */
    void close() {
        statusBar.close();
        secondaryViewContainer.close();
        bottomPanel.close();
        primaryViewContainer.close();
    }

    /** Creates compact product, menu, project-context, and command chrome. */
    private HBox createTopChrome(Runnable openProject) {
        Label productName = new Label("JScene3D");
        productName.getStyleClass().add(EditorStyleClasses.EDITOR_PRODUCT_NAME);
        Label productKind = new Label("EDITOR");
        productKind.getStyleClass().add(EditorStyleClasses.EDITOR_PRODUCT_KIND);

        MenuItem openProjectItem = new MenuItem("Open Project…");
        openProjectItem.setOnAction(ignored -> openProject.run());
        Menu file = new Menu("File");
        file.getItems().add(openProjectItem);
        MenuBar menuBar = new MenuBar(file);
        menuBar.getStyleClass().add(EditorStyleClasses.EDITOR_MENU_BAR);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        projectContext.setMaxWidth(300.0);
        projectContext.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        projectContext.getStyleClass().add(EditorStyleClasses.EDITOR_PROJECT_CONTEXT);
        Button openButton = new Button("Open Project…");
        openButton.setOnAction(ignored -> openProject.run());
        openButton.getStyleClass().add(EditorStyleClasses.EDITOR_OPEN_PROJECT_BUTTON);

        HBox chrome = new HBox(8.0, productName, productKind, menuBar, spacer, projectContext, openButton);
        chrome.setAlignment(Pos.CENTER_LEFT);
        chrome.getStyleClass().add(EditorStyleClasses.EDITOR_TOP);
        return chrome;
    }

    /** Clears UI and shared selection state before project-owned values are replaced. */
    private void clearSelection() {
        selections.clear();
    }

    /** Creates the Hierarchy and preview row above the shared lower browser. */
    private static SplitPane createUpperWorkspaceSplit(VBox hierarchyPanel, VBox previewPanel) {
        SplitPane split = new SplitPane(hierarchyPanel, previewPanel);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setMinWidth(EditorWorkspaceLayout.MINIMUM_LEFT_WORKSPACE_WIDTH);
        split.getStyleClass().add(EditorStyleClasses.EDITOR_UPPER_WORKSPACE_SPLIT);
        SplitPane.setResizableWithParent(hierarchyPanel, false);
        return split;
    }

    /** Creates the left workspace whose lower drawer spans Hierarchy and preview. */
    private SplitPane createLeftWorkspaceSplit(SplitPane upperWorkspace, JavaFxPanelPart panel) {
        SplitPane split = new SplitPane(upperWorkspace, panel.node());
        split.setOrientation(Orientation.VERTICAL);
        split.setMinWidth(EditorWorkspaceLayout.MINIMUM_LEFT_WORKSPACE_WIDTH);
        split.getStyleClass().add(EditorStyleClasses.EDITOR_LEFT_WORKSPACE_SPLIT);
        SplitPane.setResizableWithParent(panel.node(), false);
        panel.attach(split);
        return split;
    }

    /** Adds truthful preview context and leaves command space empty until commands exist. */
    private VBox createViewportPane(GLCanvas viewportCanvas) {
        previewTitle.getStyleClass().add(EditorStyleClasses.EDITOR_PREVIEW_TITLE);
        Label inertBadge = new Label("INERT");
        inertBadge
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_READ_ONLY_BADGE, EditorStyleClasses.EDITOR_INERT_BADGE);
        Region commandSpace = new Region();
        HBox.setHgrow(commandSpace, Priority.ALWAYS);
        commandSpace.getStyleClass().add(EditorStyleClasses.EDITOR_VIEWPORT_COMMAND_SPACE);
        HBox header = new HBox(8.0, previewTitle, inertBadge, commandSpace);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add(EditorStyleClasses.EDITOR_VIEWPORT_HEADER);

        StackPane viewport = new StackPane(viewportCanvas);
        viewport.setMinHeight(EditorWorkspaceLayout.MINIMUM_PREVIEW_HEIGHT);
        viewport.getStyleClass().add(EditorStyleClasses.EDITOR_VIEWPORT);
        VBox.setVgrow(viewport, Priority.ALWAYS);
        VBox panel = new VBox(header, viewport);
        panel.getStyleClass().add(EditorStyleClasses.EDITOR_VIEWPORT_PANEL);
        return panel;
    }
}
