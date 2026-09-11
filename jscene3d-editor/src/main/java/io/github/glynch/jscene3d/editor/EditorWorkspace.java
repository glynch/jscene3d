/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import io.github.glynch.jscene3d.editor.builtin.project.ProjectAsset;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.view.JavaFxViewContainer;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
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
    private final EditorSelectionModel selectionModel;
    private final Label projectContext = new Label("No project");
    private final Label previewTitle = new Label("Empty Preview");
    private final Label projectStatus = createStatus("No project opened");
    private final Label viewportStatus = createStatus("Preview: starting");
    private final Button diagnosticStatus = createStatusAction("✕ 0   △ 0");
    private final VBox inspectorEmpty = new VBox();
    private final VBox inspectorSelected = new VBox();
    private final VBox inspectorSections = new VBox();
    private final Label inspectorTitle = new Label();
    private final Label inspectorKind = new Label();
    private final Label inspectorSource = new Label();
    private final Label generatedBadge = new Label("GENERATED");
    private final ScrollPane inspectorScroll = new ScrollPane();
    private final SplitPane workspaceSplit;
    private final SplitPane upperWorkspaceSplit;
    private final SplitPane leftWorkspaceSplit;
    private final EditorBottomDrawer bottomDrawer;
    private final JavaFxViewContainer primaryViewContainer;
    private final JavaFxViewContainer bottomViewContainer;
    private List<ProjectAsset> projectAssets = List.of();

    /** Creates the shell around an existing viewport and the real open-project command. */
    EditorWorkspace(
            GLCanvas viewportCanvas,
            Runnable openProject,
            EditorSelectionModel selectionModel,
            EditorExtensionHost extensions) {
        this.selectionModel = Objects.requireNonNull(selectionModel, "selectionModel");
        setTop(createTopChrome(openProject));

        primaryViewContainer = new JavaFxViewContainer(extensions, EditorViewContainers.PRIMARY_SIDEBAR);
        VBox hierarchyPanel = primaryViewContainer.node();
        hierarchyPanel.setMinWidth(EditorWorkspaceLayout.MINIMUM_HIERARCHY_WIDTH);
        hierarchyPanel.setPrefWidth(EditorWorkspaceLayout.PREFERRED_HIERARCHY_WIDTH);
        hierarchyPanel.getStyleClass().addAll("editor-panel", "editor-hierarchy-panel");
        VBox inspectorPanel = createInspector();
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

    /** Creates the read-only Inspector driven by the shared editor selection. */
    private VBox createInspector() {
        Label emptyTitle = new Label("Nothing selected");
        emptyTitle.getStyleClass().add("editor-empty-title");
        Label emptyDetail = new Label("Select an authored entity or asset to inspect it.");
        emptyDetail.getStyleClass().add("editor-empty-detail");
        inspectorEmpty.getChildren().setAll(emptyTitle, emptyDetail);
        inspectorEmpty.setSpacing(10.0);
        inspectorEmpty.getStyleClass().add("editor-inspector-empty");

        inspectorTitle.getStyleClass().add("editor-inspector-title");
        inspectorKind.getStyleClass().add("editor-inspector-kind");
        inspectorSource.getStyleClass().add("editor-inspector-source");
        inspectorSource.setWrapText(true);
        Label readOnlyBadge = new Label("READ-ONLY");
        readOnlyBadge.getStyleClass().add("editor-read-only-badge");
        generatedBadge.getStyleClass().add("editor-generated-badge");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox titleLine = new HBox(6.0, inspectorTitle, headerSpacer, readOnlyBadge, generatedBadge);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        VBox header = new VBox(4.0, titleLine, inspectorKind, inspectorSource);
        header.getStyleClass().add("editor-inspector-header");
        inspectorSections.setSpacing(4.0);
        inspectorSections.getStyleClass().add("editor-inspector-sections");
        inspectorSelected.getChildren().setAll(header, inspectorSections);
        inspectorSelected.getStyleClass().add("editor-inspector-selected");

        inspectorScroll.setContent(inspectorSelected);
        inspectorScroll.setFitToWidth(true);
        inspectorScroll.getStyleClass().add("editor-inspector-scroll");
        StackPane body = new StackPane(inspectorEmpty, inspectorScroll);
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox inspector = new VBox(createPanelHeading("Inspector"), body);
        inspector.setMinWidth(EditorWorkspaceLayout.MINIMUM_INSPECTOR_WIDTH);
        inspector.setPrefWidth(EditorWorkspaceLayout.PREFERRED_INSPECTOR_WIDTH);
        inspector.getStyleClass().addAll("editor-panel", "editor-inspector-panel");
        selectionModel.subscribe(this::selectionChanged);
        return inspector;
    }

    /** Keeps the Inspector synchronized with the selection shared by contributed views. */
    private void selectionChanged(Optional<EditorSelection> selected) {
        showInspection(selected);
    }

    /** Clears UI and shared selection state before project-owned values are replaced. */
    private void clearSelection() {
        selectionModel.clear();
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
                    .ifPresent(item -> selectionModel.select(item.selection()));
        } catch (IllegalArgumentException ignored) {
            // An unusual file URI remains inspectable in Diagnostics without false navigation.
        }
    }

    /** Replaces the complete Inspector atomically for one selection transition. */
    private void showInspection(Optional<EditorSelection> selected) {
        boolean present = selected.isPresent();
        inspectorEmpty.setManaged(!present);
        inspectorEmpty.setVisible(!present);
        inspectorScroll.setManaged(present);
        inspectorScroll.setVisible(present);
        if (!present) {
            inspectorSections.getChildren().clear();
            return;
        }
        EditorInspectorView inspection = selected.orElseThrow().inspector();
        inspectorTitle.setText(inspection.title());
        inspectorTitle.setTooltip(new Tooltip(inspection.identity()));
        inspectorKind.setText(inspection.kind());
        inspectorSource.setText(inspection.source());
        inspectorSource.setTooltip(new Tooltip(inspection.source()));
        generatedBadge.setManaged(inspection.generated());
        generatedBadge.setVisible(inspection.generated());
        inspectorSections
                .getChildren()
                .setAll(inspection.sections().stream()
                        .map(EditorWorkspace::createInspectorSection)
                        .toList());
        inspectorScroll.setVvalue(0.0);
    }

    /** Creates one collapsible component or summary section. */
    private static javafx.scene.control.TitledPane createInspectorSection(EditorInspectorView.Section section) {
        VBox content = new VBox(6.0);
        content.getStyleClass().add("editor-inspector-section-content");
        section.description().ifPresent(description -> {
            Label detail = new Label(description);
            detail.setWrapText(true);
            detail.getStyleClass().add("editor-inspector-description");
            content.getChildren().add(detail);
        });
        if (!section.metadataAvailable()) {
            Label unavailable = new Label("Descriptor metadata unavailable");
            unavailable.getStyleClass().add("editor-inspector-metadata-warning");
            content.getChildren().add(unavailable);
        }
        section.properties().stream()
                .map(EditorWorkspace::createInspectorProperty)
                .forEach(content.getChildren()::add);
        javafx.scene.control.TitledPane pane = new javafx.scene.control.TitledPane(section.title(), content);
        pane.setAnimated(false);
        pane.setExpanded(true);
        pane.getStyleClass().add("editor-inspector-section");
        return pane;
    }

    /** Creates one typed property row without exposing an editable control. */
    private static VBox createInspectorProperty(EditorInspectorView.Property property) {
        Label name = new Label(property.displayName());
        name.getStyleClass().add("editor-inspector-property-name");
        Label value = new Label(property.value());
        value.setMaxWidth(Double.MAX_VALUE);
        value.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        value.getStyleClass().add("editor-inspector-value");
        if (property.origin() == EditorInspectorView.ValueOrigin.DEFAULT) {
            value.getStyleClass().add("editor-inspector-default-value");
        } else if (property.origin() == EditorInspectorView.ValueOrigin.UNSET) {
            value.getStyleClass().add("editor-inspector-unset-value");
        }
        String metadata = propertyMetadata(property);
        Label type = new Label(metadata);
        type.getStyleClass().add("editor-inspector-property-metadata");
        String tooltip = propertyTooltip(property);
        if (!tooltip.isEmpty()) {
            Tooltip help = new Tooltip(tooltip);
            name.setTooltip(help);
            value.setTooltip(help);
        }
        VBox row = new VBox(3.0, name, value, type);
        row.getStyleClass().add("editor-inspector-property");
        return row;
    }

    /** Formats compact value-kind and origin metadata. */
    private static String propertyMetadata(EditorInspectorView.Property property) {
        StringBuilder text = new StringBuilder(EditorInspectorProjector.label(property.valueKind()));
        if (property.required()) {
            text.append(" · required");
        }
        if (property.origin() == EditorInspectorView.ValueOrigin.DEFAULT) {
            text.append(" · default");
        } else if (property.origin() == EditorInspectorView.ValueOrigin.UNSET) {
            text.append(" · not set");
        }
        return text.toString();
    }

    /** Formats descriptions and generic constraints for on-demand inspection. */
    private static String propertyTooltip(EditorInspectorView.Property property) {
        StringBuilder text = new StringBuilder();
        property.description().ifPresent(text::append);
        for (Map.Entry<String, String> constraint : property.constraints().entrySet()) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(constraint.getKey()).append(": ").append(constraint.getValue());
        }
        return text.toString();
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

    /** Creates a consistently styled heading for one editor panel. */
    private static Label createPanelHeading(String text) {
        Label heading = new Label(text);
        heading.getStyleClass().add("editor-panel-heading");
        return heading;
    }
}
