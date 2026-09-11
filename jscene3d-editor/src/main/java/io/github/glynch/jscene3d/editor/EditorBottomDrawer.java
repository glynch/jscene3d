/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** Owns the collapsible Project and Diagnostics drawer behind a compact interface. */
final class EditorBottomDrawer extends VBox {
    private static final double HEADER_HEIGHT = 34.0;

    private final EditorDiagnosticsModel diagnosticsModel = new EditorDiagnosticsModel();
    private final Consumer<ProjectDiagnostic> diagnosticSelection;
    private final Node projectContent;
    private final VBox diagnosticsContent;
    private final StackPane content = new StackPane();
    private final Button projectTab = new Button("Project");
    private final Button diagnosticsTab = new Button();
    private final Label diagnosticsTabText = new Label("Diagnostics");
    private final Label diagnosticsBadge = new Label();
    private final ToggleButton errors = new ToggleButton();
    private final ToggleButton warnings = new ToggleButton();
    private final TextField filter = new TextField();
    private final HBox diagnosticTools;
    private final Button collapse = new Button("×");
    private final TreeView<DiagnosticNode> diagnosticTree = new TreeView<>();
    private final Label diagnosticEmpty = new Label("No project diagnostics");

    private @Nullable SplitPaneHost host;
    private Section selected = Section.PROJECT;
    private boolean expanded = true;
    private double expandedHeight = EditorWorkspaceLayout.PREFERRED_BOTTOM_HEIGHT;

    /** Creates a drawer around the existing Project browser and diagnostic navigation callback. */
    EditorBottomDrawer(Node projectContent, Consumer<ProjectDiagnostic> diagnosticSelection) {
        this.projectContent = Objects.requireNonNull(projectContent, "projectContent");
        this.diagnosticSelection = Objects.requireNonNull(diagnosticSelection, "diagnosticSelection");
        diagnosticsContent = createDiagnosticsContent();
        diagnosticTools = createDiagnosticTools();
        getChildren().setAll(createHeader(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
        setMinHeight(EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT);
        setPrefHeight(EditorWorkspaceLayout.PREFERRED_BOTTOM_HEIGHT);
        getStyleClass().add("editor-bottom-drawer");
        showSection(Section.PROJECT);
        refreshDiagnostics();
    }

    /** Attaches divider movement to the vertical workspace that owns this drawer. */
    void attach(javafx.scene.control.SplitPane splitPane) {
        Objects.requireNonNull(splitPane, "splitPane");
        if (host != null) {
            throw new IllegalStateException("drawer is already attached");
        }
        host = new SplitPaneHost(splitPane);
        splitPane.getDividers().getFirst().positionProperty().addListener((ignored, previous, current) -> {
            if (expanded && splitPane.getHeight() > 0.0) {
                double candidate = splitPane.getHeight() * (1.0 - current.doubleValue());
                if (candidate >= EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT) {
                    expandedHeight = candidate;
                }
            }
        });
    }

    /** Replaces all diagnostics and returns their complete severity counts. */
    DiagnosticCounts showDiagnostics(List<ProjectDiagnostic> projectDiagnostics) {
        diagnosticsModel.showDiagnostics(projectDiagnostics);
        refreshDiagnostics();
        EditorDiagnosticsModel.View view = diagnosticsModel.view();
        return new DiagnosticCounts(view.errors(), view.warnings());
    }

    /** Opens the Diagnostics section without toggling an already-open drawer closed. */
    void openDiagnostics() {
        selected = Section.DIAGNOSTICS;
        expand();
    }

    /** Creates the VS Code-inspired drawer header without introducing generic docking. */
    private HBox createHeader() {
        projectTab.setOnAction(ignored -> selectOrToggle(Section.PROJECT));
        projectTab.getStyleClass().add("editor-drawer-tab");

        diagnosticsTabText.getStyleClass().add("editor-drawer-tab-label");
        diagnosticsBadge.getStyleClass().add("editor-diagnostic-badge");
        HBox diagnosticsGraphic = new HBox(6.0, diagnosticsTabText, diagnosticsBadge);
        diagnosticsGraphic.setAlignment(Pos.CENTER_LEFT);
        diagnosticsTab.setGraphic(diagnosticsGraphic);
        diagnosticsTab.setOnAction(ignored -> selectOrToggle(Section.DIAGNOSTICS));
        diagnosticsTab.getStyleClass().add("editor-drawer-tab");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        collapse.setAccessibleText("Collapse bottom drawer");
        collapse.setTooltip(new Tooltip("Collapse panel"));
        collapse.setOnAction(ignored -> collapse());
        collapse.getStyleClass().add("editor-drawer-close");
        HBox header = new HBox(projectTab, diagnosticsTab, spacer, diagnosticTools, collapse);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinHeight(HEADER_HEIGHT);
        header.setPrefHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.getStyleClass().add("editor-drawer-header");
        return header;
    }

    /** Creates filtering and severity controls shown only for Diagnostics. */
    private HBox createDiagnosticTools() {
        errors.setSelected(true);
        warnings.setSelected(true);
        errors.setTooltip(new Tooltip("Show errors"));
        warnings.setTooltip(new Tooltip("Show warnings"));
        errors.setAccessibleText("Show errors");
        warnings.setAccessibleText("Show warnings");
        errors.getStyleClass().addAll("editor-diagnostic-severity-filter", "diagnostic-error");
        warnings.getStyleClass().addAll("editor-diagnostic-severity-filter", "diagnostic-warning");
        errors.setOnAction(ignored -> {
            diagnosticsModel.showSeverity(ProjectDiagnostic.Severity.ERROR, errors.isSelected());
            refreshDiagnostics();
        });
        warnings.setOnAction(ignored -> {
            diagnosticsModel.showSeverity(ProjectDiagnostic.Severity.WARNING, warnings.isSelected());
            refreshDiagnostics();
        });
        filter.setPromptText("Filter diagnostics…");
        filter.setPrefWidth(210.0);
        filter.getStyleClass().add("editor-diagnostic-filter");
        filter.textProperty().addListener((ignored, previous, current) -> {
            diagnosticsModel.filter(current);
            refreshDiagnostics();
        });
        HBox tools = new HBox(5.0, errors, warnings, filter);
        tools.setAlignment(Pos.CENTER_RIGHT);
        tools.getStyleClass().add("editor-diagnostic-tools");
        return tools;
    }

    /** Creates the grouped diagnostic tree and its intentional empty state. */
    private VBox createDiagnosticsContent() {
        diagnosticEmpty.getStyleClass().addAll("editor-empty-detail", "editor-diagnostic-empty");
        diagnosticTree.setShowRoot(false);
        diagnosticTree.setCellFactory(ignored -> new DiagnosticTreeCell());
        diagnosticTree.getSelectionModel().selectedItemProperty().addListener((ignored, previous, current) -> {
            if (current != null && current.getValue() instanceof DiagnosticItemNode(EditorDiagnosticsModel.Item item)) {
                diagnosticSelection.accept(item.diagnostic());
            }
        });
        diagnosticTree.getStyleClass().add("editor-diagnostic-tree");
        StackPane body = new StackPane(diagnosticTree, diagnosticEmpty);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox panel = new VBox(body);
        panel.getStyleClass().addAll("editor-panel", "editor-diagnostics-panel");
        return panel;
    }

    /** Selects a different section, or collapses the selected open section. */
    private void selectOrToggle(Section section) {
        if (expanded && selected == section) {
            collapse();
            return;
        }
        selected = section;
        expand();
    }

    /** Opens the selected section and restores the drawer's previous expanded height. */
    private void expand() {
        expanded = true;
        setMinHeight(EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT);
        setMaxHeight(Double.MAX_VALUE);
        setPrefHeight(expandedHeight);
        content.setManaged(true);
        content.setVisible(true);
        collapse.setManaged(true);
        collapse.setVisible(true);
        showSection(selected);
        Platform.runLater(() -> moveDivider(expandedHeight));
    }

    /** Collapses the body while leaving its tabs visible as a compact drawer handle. */
    private void collapse() {
        if (!expanded) {
            return;
        }
        if (getHeight() >= EditorWorkspaceLayout.MINIMUM_BOTTOM_HEIGHT) {
            expandedHeight = getHeight();
        }
        expanded = false;
        content.setManaged(false);
        content.setVisible(false);
        diagnosticTools.setManaged(false);
        diagnosticTools.setVisible(false);
        collapse.setManaged(false);
        collapse.setVisible(false);
        setMinHeight(HEADER_HEIGHT);
        setPrefHeight(HEADER_HEIGHT);
        setMaxHeight(HEADER_HEIGHT);
        updateTabs();
        Platform.runLater(() -> moveDivider(HEADER_HEIGHT));
    }

    /** Displays exactly one drawer body and its contextual toolbar. */
    private void showSection(Section section) {
        boolean showProject = section == Section.PROJECT;
        projectContent.setManaged(showProject);
        projectContent.setVisible(showProject);
        diagnosticsContent.setManaged(!showProject);
        diagnosticsContent.setVisible(!showProject);
        content.getChildren().setAll(projectContent, diagnosticsContent);
        diagnosticTools.setManaged(expanded && !showProject);
        diagnosticTools.setVisible(expanded && !showProject);
        updateTabs();
    }

    /** Moves the owning split divider to one bounded bottom-region height. */
    private void moveDivider(double bottomHeight) {
        if (host != null && host.splitPane().getHeight() > 0.0) {
            host.splitPane()
                    .setDividerPositions(EditorWorkspaceLayout.verticalForBottomHeight(
                            host.splitPane().getHeight(), bottomHeight));
        }
    }

    /** Updates active tabs and count/filter feedback from the current model snapshot. */
    private void refreshDiagnostics() {
        EditorDiagnosticsModel.View view = diagnosticsModel.view();
        diagnosticsBadge.setText(Long.toString(view.total()));
        diagnosticsBadge.setManaged(view.total() > 0L);
        diagnosticsBadge.setVisible(view.total() > 0L);
        diagnosticsTab.setAccessibleText("Diagnostics, " + view.total() + " issues");
        errors.setText("✕ " + view.errors());
        warnings.setText("△ " + view.warnings());
        diagnosticEmpty.setText(
                view.total() == 0L ? "No project diagnostics" : "No diagnostics match the current filter.");
        boolean empty = view.visible() == 0L;
        diagnosticEmpty.setManaged(empty);
        diagnosticEmpty.setVisible(empty);
        diagnosticTree.setManaged(!empty);
        diagnosticTree.setVisible(!empty);
        TreeItem<DiagnosticNode> root = new TreeItem<>();
        for (EditorDiagnosticsModel.Group group : view.groups()) {
            TreeItem<DiagnosticNode> groupItem = new TreeItem<>(new DiagnosticGroupNode(group));
            for (EditorDiagnosticsModel.Item item : group.items()) {
                TreeItem<DiagnosticNode> itemNode = new TreeItem<>(new DiagnosticItemNode(item));
                item.details().stream()
                        .map(detail -> new TreeItem<DiagnosticNode>(new DiagnosticDetailNode(detail)))
                        .forEach(itemNode.getChildren()::add);
                groupItem.getChildren().add(itemNode);
            }
            groupItem.setExpanded(true);
            root.getChildren().add(groupItem);
        }
        diagnosticTree.setRoot(root);
    }

    /** Applies explicit active style classes without relying on undocumented tab internals. */
    private void updateTabs() {
        projectTab.getStyleClass().remove("editor-drawer-tab-active");
        diagnosticsTab.getStyleClass().remove("editor-drawer-tab-active");
        Button active = selected == Section.PROJECT ? projectTab : diagnosticsTab;
        active.getStyleClass().add("editor-drawer-tab-active");
    }

    /** Renders source groups, concise diagnostics, and nested details in one tree. */
    private static final class DiagnosticTreeCell extends TreeCell<DiagnosticNode> {
        /** Creates a cell styled by node kind. */
        private DiagnosticTreeCell() {
            getStyleClass().add("editor-diagnostic-tree-cell");
        }

        @Override
        protected void updateItem(@Nullable DiagnosticNode node, boolean empty) {
            super.updateItem(node, empty);
            getStyleClass().removeAll("diagnostic-group", "diagnostic-item", "diagnostic-detail");
            setText(null);
            if (empty || node == null) {
                setGraphic(null);
                return;
            }
            if (node instanceof DiagnosticGroupNode(EditorDiagnosticsModel.Group group)) {
                getStyleClass().add("diagnostic-group");
                setGraphic(createGroupGraphic(group));
            } else if (node instanceof DiagnosticItemNode(EditorDiagnosticsModel.Item item)) {
                getStyleClass().add("diagnostic-item");
                setGraphic(createItemGraphic(item));
            } else if (node instanceof DiagnosticDetailNode(EditorDiagnosticsModel.Detail detail)) {
                getStyleClass().add("diagnostic-detail");
                setGraphic(createDetailGraphic(detail));
            }
        }

        /** Creates one compact source heading with severity totals. */
        private static HBox createGroupGraphic(EditorDiagnosticsModel.Group group) {
            Label source = new Label(group.label());
            source.setTooltip(new Tooltip(group.source().toString()));
            source.getStyleClass().add("editor-diagnostic-source-name");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label counts = new Label("✕ " + group.errors() + "   △ " + group.warnings());
            counts.getStyleClass().add("editor-diagnostic-group-counts");
            HBox row = new HBox(8.0, source, spacer, counts);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            return row;
        }

        /** Creates one concise diagnostic row with code and JSON Pointer location. */
        private static HBox createItemGraphic(EditorDiagnosticsModel.Item item) {
            boolean error = item.diagnostic().severity() == ProjectDiagnostic.Severity.ERROR;
            Label severity = new Label(error ? "✕" : "△");
            severity.getStyleClass()
                    .addAll("editor-diagnostic-severity", error ? "diagnostic-error" : "diagnostic-warning");
            Label message = new Label(item.summary());
            message.setMinWidth(0.0);
            message.setMaxWidth(Double.MAX_VALUE);
            message.setPrefWidth(1.0);
            message.setTooltip(new Tooltip(item.summary()));
            message.getStyleClass().add("editor-diagnostic-message");
            HBox.setHgrow(message, Priority.ALWAYS);
            Label code = new Label(item.diagnostic().code().code());
            code.getStyleClass().add("editor-diagnostic-code");
            Label location = new Label(item.diagnostic().location());
            location.setManaged(!item.diagnostic().location().isEmpty());
            location.setVisible(!item.diagnostic().location().isEmpty());
            location.getStyleClass().add("editor-diagnostic-location");
            HBox row = new HBox(8.0, severity, message, code, location);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            return row;
        }

        /** Creates one indented labelled detail row. */
        private static HBox createDetailGraphic(EditorDiagnosticsModel.Detail detail) {
            Label label = new Label(detail.label());
            label.getStyleClass().add("editor-diagnostic-detail-label");
            Label value = new Label(detail.value());
            value.setMinWidth(0.0);
            value.setMaxWidth(Double.MAX_VALUE);
            value.setPrefWidth(1.0);
            value.setTooltip(new Tooltip(detail.value()));
            value.getStyleClass().add("editor-diagnostic-detail-value");
            HBox.setHgrow(value, Priority.ALWAYS);
            HBox row = new HBox(8.0, label, value);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            return row;
        }
    }

    /** Active drawer section. */
    private enum Section {
        PROJECT,
        DIAGNOSTICS
    }

    /** Values rendered by the diagnostic tree. */
    private sealed interface DiagnosticNode permits DiagnosticGroupNode, DiagnosticItemNode, DiagnosticDetailNode {}

    /** One collapsible source group. */
    private record DiagnosticGroupNode(EditorDiagnosticsModel.Group group) implements DiagnosticNode {}

    /** One diagnostic capable of revealing structured details. */
    private record DiagnosticItemNode(EditorDiagnosticsModel.Item item) implements DiagnosticNode {}

    /** One expanded diagnostic detail. */
    private record DiagnosticDetailNode(EditorDiagnosticsModel.Detail detail) implements DiagnosticNode {}

    /** The sole parent interaction needed for restoring divider position. */
    private record SplitPaneHost(javafx.scene.control.SplitPane splitPane) {}

    /** Complete diagnostic totals returned to the workspace status bar. */
    record DiagnosticCounts(long errors, long warnings) {
        /** Returns the complete diagnostic total. */
        long total() {
            return errors + warnings;
        }
    }
}
