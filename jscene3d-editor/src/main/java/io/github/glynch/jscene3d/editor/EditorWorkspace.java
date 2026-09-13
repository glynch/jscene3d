/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import io.github.glynch.jscene3d.editor.builtin.hierarchy.HierarchyExtension;
import io.github.glynch.jscene3d.editor.command.EditorCommands;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.activity.EditorActivitySelection;
import io.github.glynch.jscene3d.editor.workbench.activity.JavaFxActivityBar;
import io.github.glynch.jscene3d.editor.workbench.appearance.JavaFxColorSchemeToggle;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.layout.JavaFxLayoutCustomizer;
import io.github.glynch.jscene3d.editor.workbench.layout.JavaFxLayoutQuickAccess;
import io.github.glynch.jscene3d.editor.workbench.layout.JavaFxWorkbenchRegions;
import io.github.glynch.jscene3d.editor.workbench.menu.JavaFxMenuBar;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusBarPane;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxContributedEditors;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxEditorArea;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxPanelPart;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxViewContainer;
import java.nio.file.Path;
import java.util.Objects;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Composes the editor workbench regions and coordinates project presentation. */
public final class EditorWorkspace extends BorderPane {
    private static final double TOP_CHROME_MARK_SIZE = 24.0;

    private final Label projectContext = new Label("No project");
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
    private final JavaFxContributedEditors contributedEditors;
    private final EditorWorkspaceDocument documentController;
    private final JavaFxWorkbenchRegions regions;
    private final EditorStatusBarPane statusBar;
    private final JavaFxColorSchemeToggle colorSchemeToggle;
    private final JavaFxMenuBar menuBar;

    /** Creates the shell around an existing viewport and the real open-project command. */
    EditorWorkspace(
            GLCanvas viewportCanvas,
            Runnable openProject,
            Runnable requestClose,
            EditorSelections selections,
            EditorExtensionHost extensions,
            EditorBuildInfo buildInfo) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        JavaFxIconRenderer icons = JavaFxIconRenderer.builtIn();
        layout = new EditorWorkbenchLayout(host);
        activitySelection = new EditorActivitySelection(host, layout, HierarchyExtension.ACTIVITY_ID);
        primaryViewContainer = new JavaFxViewContainer(host, layout, EditorViewContainers.PRIMARY_SIDEBAR, icons);
        activitySelectionRegistration = activitySelection.observe(primaryViewContainer::showActivity);
        VBox hierarchyPanel = primaryViewContainer.node();
        hierarchyPanel
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_HIERARCHY_PANEL);
        secondaryViewContainer = new JavaFxViewContainer(host, layout, EditorViewContainers.SECONDARY_SIDEBAR, icons);
        VBox inspectorPanel = secondaryViewContainer.node();
        inspectorPanel
                .getStyleClass()
                .addAll(EditorStyleClasses.EDITOR_PANEL, EditorStyleClasses.EDITOR_INSPECTOR_PANEL);
        editorArea = new JavaFxEditorArea();
        contributedEditors = new JavaFxContributedEditors(editorArea, host, layout, icons);
        bottomPanel = new JavaFxPanelPart(
                host,
                layout,
                EditorViewContainers.BOTTOM_PANEL,
                icons,
                EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT,
                EditorWorkspaceLayout.PREFERRED_BOTTOM_HEIGHT,
                EditorWorkspaceLayout::verticalForBottomHeight);
        statusBar = new EditorStatusBarPane(host, icons);
        activityBar = new JavaFxActivityBar(host, activitySelection, icons);
        regions = new JavaFxWorkbenchRegions(
                layout,
                activityBar.node(),
                hierarchyPanel,
                editorArea.node(),
                bottomPanel,
                inspectorPanel,
                statusBar.node());
        layoutCustomizer = new JavaFxLayoutCustomizer(layout, icons, host::showView);
        layoutQuickAccess = new JavaFxLayoutQuickAccess(layout, icons, layoutCustomizer.button());
        documentController = new EditorWorkspaceDocument(
                new EditorWorkspaceDocument.Context(
                        host,
                        Objects.requireNonNull(selections, "selections"),
                        editorArea,
                        icons,
                        createViewportContent(viewportCanvas),
                        projectContext,
                        statusBar),
                new EditorWorkspaceDocument.Actions(openProject, requestClose, buildInfo));
        colorSchemeToggle = new JavaFxColorSchemeToggle(
                host.colorThemes(), icons, () -> host.execute(EditorCommands.TOGGLE_COLOR_SCHEME));
        menuBar = new JavaFxMenuBar(host);
        setTop(createTopChrome(menuBar.node(), colorSchemeToggle.button(), layoutQuickAccess.node()));
        setCenter(regions.node());
        getStyleClass().add(EditorStyleClasses.EDITOR_SHELL);
    }

    /** Applies bounded initial divider positions after the stage has completed its first layout. */
    void applyInitialDividerPositions() {
        regions.applyInitialDividerPositions();
    }

    /** Opens the Welcome editor when startup completes without a requested project. */
    void showWelcome() {
        documentController.showWelcome();
    }

    /** Updates the viewport portion of the status bar. */
    void setViewportStatus(String text) {
        statusBar.showViewportStatus(text);
    }

    /** Shows that a project directory is being opened without claiming it has loaded. */
    public void beginOpening(Path directory) {
        documentController.beginOpening(directory);
    }

    /** Replaces the visible hierarchy, Project content, and preview context atomically. */
    public void showProject(EditorProjectSession session) {
        documentController.showProject(session);
        activitySelection.revealDefault();
    }

    /** Clears project-owned views after an unsuccessful open. */
    public void clearProject() {
        documentController.clearProject();
    }

    /** Updates the concise project portion of the status bar. */
    public void setProjectStatus(String text) {
        documentController.setProjectStatus(text);
    }

    /** Completes project presentation with the default project navigation visible. */
    public void finishProjectOpening(String status) {
        activitySelection.revealDefault();
        statusBar.showProjectStatus(status);
    }

    /** Shows an extension message without exposing JavaFX through the extension interface. */
    public void showMessage(EditorMessage message) {
        documentController.showMessage(message);
    }

    /** Confirms or saves dirty resources before an orderly window close. */
    boolean prepareToClose() {
        return documentController.prepareToClose();
    }

    /** Releases workbench adapters before the extension host is closed. */
    void close() {
        menuBar.close();
        colorSchemeToggle.close();
        layoutQuickAccess.close();
        layoutCustomizer.close();
        regions.close();
        activityBar.close();
        activitySelectionRegistration.close();
        activitySelection.close();
        contributedEditors.close();
        documentController.close();
        editorArea.close();
        statusBar.close();
        secondaryViewContainer.close();
        bottomPanel.close();
        primaryViewContainer.close();
        layout.close();
    }

    private HBox createTopChrome(MenuBar menus, Button themeToggle, HBox layoutActions) {
        EditorBrandMark productMark = new EditorBrandMark(TOP_CHROME_MARK_SIZE);
        productMark.getStyleClass().add(EditorStyleClasses.EDITOR_PRODUCT_MARK);
        Label productKind = new Label("EDITOR");
        productKind.getStyleClass().add(EditorStyleClasses.EDITOR_PRODUCT_KIND);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        projectContext.setMaxWidth(300.0);
        projectContext.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        projectContext.getStyleClass().add(EditorStyleClasses.EDITOR_PROJECT_CONTEXT);
        HBox chrome =
                new HBox(8.0, productMark, productKind, menus, spacer, projectContext, themeToggle, layoutActions);
        chrome.setAlignment(Pos.CENTER_LEFT);
        chrome.getStyleClass().add(EditorStyleClasses.EDITOR_TOP);
        return chrome;
    }

    private static StackPane createViewportContent(GLCanvas viewportCanvas) {
        StackPane viewport = new StackPane(viewportCanvas);
        viewport.setMinHeight(EditorWorkspaceLayout.MINIMUM_PREVIEW_HEIGHT);
        viewport.getStyleClass().add(EditorStyleClasses.EDITOR_VIEWPORT);
        return viewport;
    }
}
