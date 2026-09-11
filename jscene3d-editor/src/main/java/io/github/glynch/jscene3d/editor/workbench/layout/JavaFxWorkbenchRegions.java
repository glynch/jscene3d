/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.layout;

import io.github.glynch.jscene3d.editor.EditorWorkspaceLayout;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxPanelPart;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

/** Applies session layout state to the major JavaFX workbench regions. */
public final class JavaFxWorkbenchRegions implements AutoCloseable {
    private final Node activityBar;
    private final Region primarySidebar;
    private final Node editorArea;
    private final JavaFxPanelPart panel;
    private final Region secondarySidebar;
    private final Node statusBar;
    private final BorderPane upper = new BorderPane();
    private final SplitPane primaryEditorSplit = new SplitPane();
    private final SplitPane editorPanelSplit = new SplitPane();
    private final BorderPane activityWorkspace = new BorderPane();
    private final SplitPane workspaceSplit = new SplitPane();
    private final BorderPane root = new BorderPane();
    private final EditorRegistration layoutRegistration;
    private EditorWorkbenchLayoutState state;

    /**
     * Creates the physical workbench layout and begins observing its session model.
     *
     * @param layout current session layout
     * @param activityBar Activity Bar node
     * @param primarySidebar primary-side-bar node
     * @param editorArea central editor-area node
     * @param panel lower panel
     * @param secondarySidebar secondary-side-bar node
     * @param statusBar status-bar node
     */
    public JavaFxWorkbenchRegions(
            EditorWorkbenchLayout layout,
            Node activityBar,
            Region primarySidebar,
            Node editorArea,
            JavaFxPanelPart panel,
            Region secondarySidebar,
            Node statusBar) {
        EditorWorkbenchLayout model = Objects.requireNonNull(layout, "layout");
        this.activityBar = Objects.requireNonNull(activityBar, "activityBar");
        this.primarySidebar = Objects.requireNonNull(primarySidebar, "primarySidebar");
        this.editorArea = Objects.requireNonNull(editorArea, "editorArea");
        this.panel = Objects.requireNonNull(panel, "panel");
        this.secondarySidebar = Objects.requireNonNull(secondarySidebar, "secondarySidebar");
        this.statusBar = Objects.requireNonNull(statusBar, "statusBar");
        state = model.current();
        configure();
        layoutRegistration = model.observe(this::apply);
    }

    /**
     * Returns the workbench-owned JavaFX region.
     *
     * @return complete region layout
     */
    public BorderPane node() {
        return root;
    }

    /** Applies bounded initial divider positions after the first JavaFX layout pass. */
    public void applyInitialDividerPositions() {
        positionDividers();
    }

    @Override
    public void close() {
        layoutRegistration.close();
        root.setCenter(null);
        root.setBottom(null);
    }

    private void configure() {
        primarySidebar.setMinWidth(EditorWorkspaceLayout.MINIMUM_PRIMARY_SIDEBAR_WIDTH);
        primarySidebar.setPrefWidth(EditorWorkspaceLayout.PREFERRED_PRIMARY_SIDEBAR_WIDTH);
        secondarySidebar.setMinWidth(EditorWorkspaceLayout.MINIMUM_SECONDARY_SIDEBAR_WIDTH);
        secondarySidebar.setPrefWidth(EditorWorkspaceLayout.PREFERRED_SECONDARY_SIDEBAR_WIDTH);
        primaryEditorSplit.setOrientation(Orientation.HORIZONTAL);
        primaryEditorSplit.setMinWidth(EditorWorkspaceLayout.MINIMUM_PRIMARY_WORKSPACE_WIDTH);
        primaryEditorSplit.getStyleClass().add(EditorStyleClasses.EDITOR_UPPER_WORKSPACE_SPLIT);
        SplitPane.setResizableWithParent(primarySidebar, false);
        upper.setCenter(primaryEditorSplit);

        editorPanelSplit.setOrientation(Orientation.VERTICAL);
        editorPanelSplit.setMinWidth(EditorWorkspaceLayout.MINIMUM_PRIMARY_WORKSPACE_WIDTH);
        editorPanelSplit.getStyleClass().add(EditorStyleClasses.EDITOR_LEFT_WORKSPACE_SPLIT);
        SplitPane.setResizableWithParent(panel.node(), false);
        panel.attach(editorPanelSplit);
        activityWorkspace.setCenter(editorPanelSplit);

        workspaceSplit.setOrientation(Orientation.HORIZONTAL);
        workspaceSplit.getStyleClass().add(EditorStyleClasses.EDITOR_WORKSPACE_SPLIT);
        SplitPane.setResizableWithParent(secondarySidebar, false);
        root.setCenter(workspaceSplit);
        apply(state);
    }

    private void apply(EditorWorkbenchLayoutState replacement) {
        state = Objects.requireNonNull(replacement, "replacement");
        activityWorkspace.setLeft(state.isVisible(EditorWorkbenchPart.ACTIVITY_BAR) ? activityBar : null);
        if (!state.isVisible(EditorWorkbenchPart.PRIMARY_SIDEBAR)) {
            primaryEditorSplit.getItems().setAll(editorArea);
        } else if (state.primarySidebarPosition() == EditorPrimarySidebarPosition.LEFT) {
            primaryEditorSplit.getItems().setAll(primarySidebar, editorArea);
        } else {
            primaryEditorSplit.getItems().setAll(editorArea, primarySidebar);
        }
        editorPanelSplit
                .getItems()
                .setAll(state.isVisible(EditorWorkbenchPart.PANEL) ? List.of(upper, panel.node()) : List.of(upper));
        workspaceSplit
                .getItems()
                .setAll(
                        state.isVisible(EditorWorkbenchPart.SECONDARY_SIDEBAR)
                                ? List.of(activityWorkspace, secondarySidebar)
                                : List.of(activityWorkspace));
        root.setBottom(state.isVisible(EditorWorkbenchPart.STATUS_BAR) ? statusBar : null);
        Platform.runLater(this::positionDividers);
    }

    private void positionDividers() {
        EditorWorkspaceLayout.HorizontalDividers horizontal =
                EditorWorkspaceLayout.horizontal(workspaceSplit.getWidth());
        if (workspaceSplit.getItems().size() > 1) {
            workspaceSplit.setDividerPositions(horizontal.secondarySidebarStart());
        }
        if (primaryEditorSplit.getItems().size() > 1) {
            double position = state.primarySidebarPosition() == EditorPrimarySidebarPosition.LEFT
                    ? horizontal.primarySidebarEnd()
                    : 1.0 - horizontal.primarySidebarEnd();
            primaryEditorSplit.setDividerPositions(position);
        }
        if (editorPanelSplit.getItems().size() > 1) {
            editorPanelSplit.setDividerPositions(EditorWorkspaceLayout.vertical(editorPanelSplit.getHeight()));
        }
    }
}
