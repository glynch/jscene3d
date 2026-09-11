/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxViewContainer;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Owns the editor's resizable JavaFX workspace and its visible read-only state. */
final class EditorWorkspace extends BorderPane {
    private final EditorSelections selections;
    private final Label projectContext = new Label("No project");
    private final Label previewTitle = new Label("Empty Preview");
    private final Label projectStatus = createStatus("No project opened");
    private final Label viewportStatus = createStatus("Preview: starting");
    private final Button diagnosticStatus = createStatusAction("✕ 0   △ 0");
    private final SplitPane workspaceSplit;
    private final SplitPane upperWorkspaceSplit;
    private final SplitPane leftWorkspaceSplit;
    private final EditorBottomDrawer bottomDrawer;
    private final JavaFxViewContainer primaryViewContainer;
    private final JavaFxViewContainer bottomViewContainer;
    private final JavaFxViewContainer secondaryViewContainer;
    private List<ProjectAsset> projectAssets = List.of();

    /** Creates the shell around an existing viewport and the real open-project command. */
    EditorWorkspace(
            GLCanvas viewportCanvas,
            Runnable openProject,
            EditorSelections selections,
            EditorExtensionHost extensions) {
        this.selections = Objects.requireNonNull(selections, "selections");
        setTop(createTopChrome(openProject));

        primaryViewContainer = new JavaFxViewContainer(extensions, EditorViewContainers.PRIMARY_SIDEBAR);
        VBox hierarchyPanel = primaryViewContainer.node();
        hierarchyPanel.setMinWidth(EditorWorkspaceLayout.MINIMUM_HIERARCHY_WIDTH);
        hierarchyPanel.setPrefWidth(EditorWorkspaceLayout.PREFERRED_HIERARCHY_WIDTH);
        hierarchyPanel.getStyleClass().addAll("editor-panel", "editor-hierarchy-panel");
        secondaryViewContainer = new JavaFxViewContainer(extensions, EditorViewContainers.SECONDARY_SIDEBAR);
        VBox inspectorPanel = secondaryViewContainer.node();
        inspectorPanel.setMinWidth(EditorWorkspaceLayout.MINIMUM_INSPECTOR_WIDTH);
        inspectorPanel.setPrefWidth(EditorWorkspaceLayout.PREFERRED_INSPECTOR_WIDTH);
        inspectorPanel.getStyleClass().addAll("editor-panel", "editor-inspector-panel");
        VBox previewPanel = createViewportPane(viewportCanvas);
        upperWorkspaceSplit = createUpperWorkspaceSplit(hierarchyPanel, previewPanel);
        bottomViewContainer = new JavaFxViewContainer(extensions, EditorViewContainers.BOTTOM_PANEL, false);
        bottomDrawer = new EditorBottomDrawer(bottomViewContainer.node(), this::selectDiagnostic);
        leftWorkspaceSplit = createLeftWorkspaceSplit(upperWorkspaceSplit, bottomDrawer);
        workspaceSplit = new SplitPane(leftWorkspaceSplit, inspectorPanel);
        workspaceSplit.setOrientation(Orientation.HORIZONTAL);
        workspaceSplit.getStyleClass().add("editor-workspace-split");
        SplitPane.setResizableWithParent(inspectorPanel, false);
        setCenter(workspaceSplit);
        setBottom(createStatusBar());
        getStyleClass().add("editor-shell");
        diagnosticStatus.setOnAction(ignored -> bottomDrawer.openDiagnostics());
    }

    /** Applies bounded initial divider positions after the stage has completed its first layout. */
    void applyInitialDividerPositions() {
        EditorWorkspaceLayout.HorizontalDividers horizontal =
                EditorWorkspaceLayout.horizontal(workspaceSplit.getWidth());
        workspaceSplit.setDividerPositions(horizontal.inspectorStart());
        upperWorkspaceSplit.setDividerPositions(horizontal.hierarchyEnd());
        leftWorkspaceSplit.setDividerPositions(EditorWorkspaceLayout.vertical(leftWorkspaceSplit.getHeight()));
    }

    /** Returns the status label updated by the OpenGL rendering coordinator. */
    Label viewportStatus() {
        return viewportStatus;
    }

    /** Shows that a project directory is being opened without claiming it has loaded. */
    void beginOpening(Path directory) {
        clearSelection();
        Path normalized = directory.toAbsolutePath().normalize();
        Path fileName = normalized.getFileName();
        String candidateName = fileName == null ? normalized.toString() : fileName.toString();
        projectAssets = List.of();
        projectContext.setText(candidateName);
        projectContext.setTooltip(new Tooltip(normalized.toString()));
        projectStatus.setText("Opening " + candidateName + "…");
    }

    /** Replaces the visible hierarchy, Project content, and preview context atomically. */
    void showProject(EditorProjectSession session) {
        clearSelection();
        String projectName = session.project().identity().name();
        projectAssets = List.copyOf(session.assets());
        projectContext.setText(projectName);
        previewTitle.setText(session.hierarchy().label() + " Preview");
    }

    /** Clears project-owned views after an unsuccessful open. */
    void clearProject() {
        clearSelection();
        projectAssets = List.of();
        projectContext.setText("No project");
        projectContext.setTooltip(null);
        previewTitle.setText("Empty Preview");
    }

    /** Replaces structured diagnostics and refreshes their concise count. */
    void showDiagnostics(List<ProjectDiagnostic> projectDiagnostics) {
        EditorBottomDrawer.DiagnosticCounts counts = bottomDrawer.showDiagnostics(projectDiagnostics);
        diagnosticStatus.getStyleClass().removeAll("editor-error", "editor-warning");
        if (counts.errors() > 0L) {
            diagnosticStatus.getStyleClass().add("editor-error");
        } else if (counts.warnings() > 0L) {
            diagnosticStatus.getStyleClass().add("editor-warning");
        }
        diagnosticStatus.setText("✕ " + counts.errors() + "   △ " + counts.warnings());
        diagnosticStatus.setAccessibleText(
                counts.errors() + " errors and " + counts.warnings() + " warnings; open Diagnostics");
    }

    /** Opens the Diagnostics drawer in response to a failed project or preview operation. */
    void openDiagnostics() {
        bottomDrawer.openDiagnostics();
    }

    /** Updates the concise project portion of the status bar. */
    void setProjectStatus(String text) {
        projectStatus.setText(text);
    }

    /** Shows an extension message without exposing JavaFX through the extension interface. */
    void showMessage(EditorMessage message) {
        EditorMessage shown = Objects.requireNonNull(message, "message");
        projectStatus.setText(shown.text());
    }

    /** Releases workbench adapters before the extension host is closed. */
    void close() {
        secondaryViewContainer.close();
        bottomViewContainer.close();
        primaryViewContainer.close();
    }

    /** Creates compact product, menu, project-context, and command chrome. */
    private HBox createTopChrome(Runnable openProject) {
        Label productName = new Label("JScene3D");
        productName.getStyleClass().add("editor-product-name");
        Label productKind = new Label("EDITOR");
        productKind.getStyleClass().add("editor-product-kind");

        MenuItem openProjectItem = new MenuItem("Open Project…");
        openProjectItem.setOnAction(ignored -> openProject.run());
        Menu file = new Menu("File");
        file.getItems().add(openProjectItem);
        MenuBar menuBar = new MenuBar(file);
        menuBar.getStyleClass().add("editor-menu-bar");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        projectContext.setMaxWidth(300.0);
        projectContext.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        projectContext.getStyleClass().add("editor-project-context");
        Button openButton = new Button("Open Project…");
        openButton.setOnAction(ignored -> openProject.run());
        openButton.getStyleClass().add("editor-open-project-button");

        HBox chrome = new HBox(8.0, productName, productKind, menuBar, spacer, projectContext, openButton);
        chrome.setAlignment(Pos.CENTER_LEFT);
        chrome.getStyleClass().add("editor-top");
        return chrome;
    }

    /** Clears UI and shared selection state before project-owned values are replaced. */
    private void clearSelection() {
        selections.clear();
    }

    /** Selects the exact Project item named by a file-backed diagnostic when one exists. */
    private void selectDiagnostic(ProjectDiagnostic diagnostic) {
        if (!"file".equalsIgnoreCase(diagnostic.source().getScheme())) {
            return;
        }
        try {
            Path source = Path.of(diagnostic.source()).toAbsolutePath().normalize();
            projectAssets.stream()
                    .filter(item -> item.source().toAbsolutePath().normalize().equals(source))
                    .findFirst()
                    .ifPresent(item -> selections.select(item.selection()));
        } catch (IllegalArgumentException ignored) {
            // An unusual file URI remains inspectable in Diagnostics without false navigation.
        }
    }

    /** Creates the Hierarchy and preview row above the shared lower browser. */
    private static SplitPane createUpperWorkspaceSplit(VBox hierarchyPanel, VBox previewPanel) {
        SplitPane split = new SplitPane(hierarchyPanel, previewPanel);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setMinWidth(EditorWorkspaceLayout.MINIMUM_LEFT_WORKSPACE_WIDTH);
        split.getStyleClass().add("editor-upper-workspace-split");
        SplitPane.setResizableWithParent(hierarchyPanel, false);
        return split;
    }

    /** Creates the left workspace whose lower drawer spans Hierarchy and preview. */
    private SplitPane createLeftWorkspaceSplit(SplitPane upperWorkspace, EditorBottomDrawer drawer) {
        SplitPane split = new SplitPane(upperWorkspace, drawer);
        split.setOrientation(Orientation.VERTICAL);
        split.setMinWidth(EditorWorkspaceLayout.MINIMUM_LEFT_WORKSPACE_WIDTH);
        split.getStyleClass().add("editor-left-workspace-split");
        SplitPane.setResizableWithParent(drawer, false);
        drawer.attach(split);
        return split;
    }

    /** Adds truthful preview context and leaves command space empty until commands exist. */
    private VBox createViewportPane(GLCanvas viewportCanvas) {
        previewTitle.getStyleClass().add("editor-preview-title");
        Label inertBadge = new Label("INERT");
        inertBadge.getStyleClass().addAll("editor-read-only-badge", "editor-inert-badge");
        Region commandSpace = new Region();
        HBox.setHgrow(commandSpace, Priority.ALWAYS);
        commandSpace.getStyleClass().add("editor-viewport-command-space");
        HBox header = new HBox(8.0, previewTitle, inertBadge, commandSpace);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("editor-viewport-header");

        StackPane viewport = new StackPane(viewportCanvas);
        viewport.setMinHeight(EditorWorkspaceLayout.MINIMUM_PREVIEW_HEIGHT);
        viewport.getStyleClass().add("editor-viewport");
        VBox.setVgrow(viewport, Priority.ALWAYS);
        VBox panel = new VBox(header, viewport);
        panel.getStyleClass().add("editor-viewport-panel");
        return panel;
    }

    /** Creates the concise persistent project, preview, and diagnostic status line. */
    private HBox createStatusBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Separator separator = new Separator(Orientation.VERTICAL);
        HBox status = new HBox(10.0, projectStatus, spacer, viewportStatus, separator, diagnosticStatus);
        status.setAlignment(Pos.CENTER_LEFT);
        status.getStyleClass().add("editor-status-bar");
        return status;
    }

    /** Creates one status label. */
    private static Label createStatus(String initialText) {
        Label status = new Label(initialText);
        status.getStyleClass().add("editor-status");
        return status;
    }

    /** Creates an unobtrusive status action whose purpose remains keyboard accessible. */
    private static Button createStatusAction(String initialText) {
        Button status = new Button(initialText);
        status.getStyleClass().addAll("editor-status", "editor-status-action");
        return status;
    }
}
