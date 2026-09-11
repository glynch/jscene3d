/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import com.huskerdev.openglfx.canvas.GLCanvas;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** Owns the editor's resizable JavaFX workspace and its visible read-only state. */
final class EditorWorkspace extends BorderPane {
    private final TreeView<EditorHierarchyNode> hierarchy = new TreeView<>();
    private final TreeView<ProjectBrowserNode> projectTree = new TreeView<>();
    private final ListView<EditorAssetItem> assets = new ListView<>();
    private final TilePane assetGrid = new TilePane();
    private final ScrollPane assetGridScroll = new ScrollPane();
    private final StackPane assetBrowserContent = new StackPane();
    private final ToggleGroup assetCardGroup = new ToggleGroup();
    private final ToggleGroup assetViewGroup = new ToggleGroup();
    private final ToggleButton assetGridView = new ToggleButton("▦");
    private final ToggleButton assetListView = new ToggleButton("☷");
    private final TextField assetSearch = new TextField();
    private final Label assetBreadcrumb = new Label("No project");
    private final Label assetBrowserEmpty = new Label();
    private final EditorSelectionModel selectionModel = new EditorSelectionModel();
    private final EditorProjectBrowserModel projectBrowser = new EditorProjectBrowserModel();
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
    private String projectBrowserProjectName = "No project";
    private boolean showingAssetGrid = true;

    /** Creates the shell around an existing viewport and the real open-project command. */
    EditorWorkspace(GLCanvas viewportCanvas, Runnable openProject) {
        setTop(createTopChrome(openProject));

        VBox hierarchyPanel = createHierarchy();
        VBox inspectorPanel = createInspector();
        VBox previewPanel = createViewportPane(viewportCanvas);
        upperWorkspaceSplit = createUpperWorkspaceSplit(hierarchyPanel, previewPanel);
        bottomDrawer = new EditorBottomDrawer(createProjectBrowser(), this::selectDiagnostic);
        leftWorkspaceSplit = createLeftWorkspaceSplit(upperWorkspaceSplit, bottomDrawer);
        workspaceSplit = new SplitPane(leftWorkspaceSplit, inspectorPanel);
        workspaceSplit.setOrientation(Orientation.HORIZONTAL);
        workspaceSplit.getStyleClass().add("editor-workspace-split");
        SplitPane.setResizableWithParent(inspectorPanel, false);
        setCenter(workspaceSplit);
        setBottom(createStatusBar());
        getStyleClass().add("editor-shell");
        installSelectionEvents();
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
        projectBrowser.clear();
        projectBrowserProjectName = candidateName;
        assetBrowserEmpty.setText("Loading project assets…");
        assetSearch.clear();
        assetSearch.setDisable(true);
        showProjectTree();
        refreshProjectBrowser();
        projectContext.setText(candidateName);
        projectContext.setTooltip(new Tooltip(normalized.toString()));
        projectStatus.setText("Opening " + candidateName + "…");
    }

    /** Replaces the visible hierarchy, Project content, and preview context atomically. */
    void showProject(EditorProjectSession session) {
        clearSelection();
        TreeItem<EditorHierarchyNode> root = createTreeItem(session.hierarchy());
        root.setExpanded(true);
        hierarchy.setRoot(root);
        String projectName = session.project().identity().name();
        projectBrowser.showProject(session.assets());
        projectBrowserProjectName = projectName;
        assetBrowserEmpty.setText("No assets match this Project location or search.");
        assetSearch.clear();
        assetSearch.setDisable(false);
        showProjectTree();
        refreshProjectBrowser();
        projectContext.setText(projectName);
        previewTitle.setText(session.hierarchy().label() + " Preview");
        TreeItem<EditorHierarchyNode> initialSelection =
                root.getChildren().isEmpty() ? root : root.getChildren().getFirst();
        hierarchy.getSelectionModel().select(initialSelection);
    }

    /** Clears project-owned views after an unsuccessful open. */
    void clearProject() {
        clearSelection();
        hierarchy.setRoot(null);
        projectBrowser.clear();
        projectTree.setRoot(null);
        projectBrowserProjectName = "No project";
        assetBrowserEmpty.setText("Open a project to browse Worlds, Entity Definitions, Source Assets, and Imports.");
        assetSearch.clear();
        assetSearch.setDisable(true);
        refreshProjectBrowser();
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

    /** Creates the authored-world hierarchy region. */
    private VBox createHierarchy() {
        hierarchy.getStyleClass().add("editor-hierarchy");
        VBox.setVgrow(hierarchy, Priority.ALWAYS);
        VBox panel = new VBox(6.0, createPanelHeading("Hierarchy"), hierarchy);
        panel.setMinWidth(EditorWorkspaceLayout.MINIMUM_HIERARCHY_WIDTH);
        panel.setPrefWidth(EditorWorkspaceLayout.PREFERRED_HIERARCHY_WIDTH);
        panel.getStyleClass().addAll("editor-panel", "editor-hierarchy-panel");
        return panel;
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
        selectionModel.subscribe(this::showInspection);
        return inspector;
    }

    /** Routes Hierarchy and Project selections through the same stable selection model. */
    private void installSelectionEvents() {
        hierarchy.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selected != null) {
                clearProjectSelection();
                selectionModel.select(selected.getValue().selection());
            }
        });
        assets.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selected != null) {
                selectProjectItem(selected);
            }
        });
        projectTree.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            if (selected != null) {
                projectBrowser.showCategory(selected.getValue().category());
                refreshProjectBrowser();
            }
        });
    }

    /** Clears UI and shared selection state before project-owned values are replaced. */
    private void clearSelection() {
        hierarchy.getSelectionModel().clearSelection();
        clearProjectSelection();
        selectionModel.clear();
    }

    /** Clears both Project presentations and their retained stable selection. */
    private void clearProjectSelection() {
        assets.getSelectionModel().clearSelection();
        assetCardGroup.selectToggle(null);
        projectBrowser.clearSelection();
    }

    /** Routes a Project item through the browser and shared editor selection models. */
    private void selectProjectItem(EditorAssetItem selected) {
        projectBrowser.select(selected);
        hierarchy.getSelectionModel().clearSelection();
        synchronizeProjectSelection();
        selectionModel.select(selected.selection());
    }

    /** Selects the exact Project item named by a file-backed diagnostic when one exists. */
    private void selectDiagnostic(ProjectDiagnostic diagnostic) {
        if (!"file".equalsIgnoreCase(diagnostic.source().getScheme())) {
            return;
        }
        try {
            projectBrowser.findBySource(Path.of(diagnostic.source())).ifPresent(this::selectProjectItem);
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

    /** Creates the categorized Project browser backed by the existing asset projection. */
    private VBox createProjectBrowser() {
        projectTree.setCellFactory(ignored -> new ProjectBrowserCell());
        projectTree.setShowRoot(true);
        projectTree.setMinWidth(0.0);
        projectTree.getStyleClass().add("editor-project-tree");
        VBox.setVgrow(projectTree, Priority.ALWAYS);
        Label categoriesHeading = new Label("Categories");
        categoriesHeading.getStyleClass().add("editor-project-categories-heading");
        VBox navigation = new VBox(categoriesHeading, projectTree);
        navigation.setMinWidth(180.0);
        navigation.setPrefWidth(230.0);
        navigation.getStyleClass().add("editor-project-navigation");

        assetBreadcrumb.setMinWidth(80.0);
        assetBreadcrumb.setMaxWidth(240.0);
        assetBreadcrumb.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        assetBreadcrumb.getStyleClass().add("editor-project-breadcrumb");
        Region toolbarSpacer = new Region();
        HBox.setHgrow(toolbarSpacer, Priority.ALWAYS);
        assetGridView.setToggleGroup(assetViewGroup);
        assetListView.setToggleGroup(assetViewGroup);
        assetGridView.setAccessibleText("Grid view");
        assetListView.setAccessibleText("List view");
        assetGridView.setTooltip(new Tooltip("Grid view"));
        assetListView.setTooltip(new Tooltip("List view"));
        assetGridView.setSelected(true);
        assetGridView.setOnAction(ignored -> showAssetView(true));
        assetListView.setOnAction(ignored -> showAssetView(false));
        assetGridView.getStyleClass().add("editor-project-view-toggle");
        assetListView.getStyleClass().add("editor-project-view-toggle");
        HBox viewButtons = new HBox(assetGridView, assetListView);
        viewButtons.getStyleClass().add("editor-project-view-buttons");
        assetSearch.setPromptText("Search assets…");
        assetSearch.setMinWidth(100.0);
        assetSearch.setPrefWidth(180.0);
        assetSearch.setDisable(true);
        assetSearch.getStyleClass().add("editor-project-search");
        assetSearch.textProperty().addListener((ignored, previous, current) -> {
            projectBrowser.search(current);
            refreshProjectBrowser();
        });
        HBox toolbar = new HBox(8.0, assetBreadcrumb, toolbarSpacer, viewButtons, assetSearch);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("editor-project-toolbar");

        assets.setCellFactory(ignored -> new AssetListCell());
        assets.getStyleClass().add("editor-asset-list");
        VBox.setVgrow(assets, Priority.ALWAYS);
        assetGrid.setHgap(8.0);
        assetGrid.setVgap(8.0);
        assetGrid.setPrefTileWidth(156.0);
        assetGrid.setPrefTileHeight(112.0);
        assetGrid.getStyleClass().add("editor-asset-grid");
        assetGridScroll.setContent(assetGrid);
        assetGridScroll.setFitToWidth(true);
        assetGridScroll.setPannable(true);
        assetGridScroll.getStyleClass().add("editor-asset-grid-scroll");
        assetBrowserEmpty.setText("Open a project to browse Worlds, Entity Definitions, Source Assets, and Imports.");
        assetBrowserEmpty.setWrapText(true);
        assetBrowserEmpty.getStyleClass().addAll("editor-empty-detail", "editor-project-empty");
        assetBrowserContent.getChildren().setAll(assets, assetGridScroll, assetBrowserEmpty);
        VBox.setVgrow(assetBrowserContent, Priority.ALWAYS);
        showAssetView(true);

        VBox content = new VBox(toolbar, assetBrowserContent);
        VBox.setVgrow(assetBrowserContent, Priority.ALWAYS);
        content.getStyleClass().add("editor-project-content");
        SplitPane browser = new SplitPane(navigation, content);
        browser.setOrientation(Orientation.HORIZONTAL);
        browser.setDividerPositions(0.22);
        browser.getStyleClass().add("editor-project-browser-split");
        SplitPane.setResizableWithParent(navigation, false);
        VBox.setVgrow(browser, Priority.ALWAYS);
        VBox panel = new VBox(browser);
        panel.getStyleClass().addAll("editor-panel", "editor-project-panel");
        return panel;
    }

    /** Selects a Project presentation while preserving the browser's stable item selection. */
    private void showAssetView(boolean showGrid) {
        showingAssetGrid = showGrid;
        assetGridView.setSelected(showGrid);
        assetListView.setSelected(!showGrid);
        refreshProjectBrowserVisibility();
        synchronizeProjectSelection();
    }

    /** Rebuilds the category tree with truthful counts for the current project. */
    private void showProjectTree() {
        ProjectBrowserNode project = ProjectBrowserNode.project(
                projectBrowserProjectName, projectBrowser.count(EditorProjectBrowserModel.Category.ALL));
        TreeItem<ProjectBrowserNode> root = new TreeItem<>(project);
        for (EditorProjectBrowserModel.Category category : EditorProjectBrowserModel.Category.values()) {
            if (category != EditorProjectBrowserModel.Category.ALL) {
                root.getChildren()
                        .add(new TreeItem<>(ProjectBrowserNode.category(category, projectBrowser.count(category))));
            }
        }
        root.setExpanded(true);
        projectTree.setRoot(root);
        projectTree.getSelectionModel().select(root);
    }

    /** Refreshes both Project presentations after navigation or search changes. */
    private void refreshProjectBrowser() {
        List<EditorAssetItem> visibleItems = projectBrowser.visibleItems();
        assets.getItems().setAll(visibleItems);
        assetCardGroup.getToggles().clear();
        assetGrid
                .getChildren()
                .setAll(visibleItems.stream().map(this::createAssetCard).toList());
        assetBreadcrumb.setText(
                projectBrowserProjectName + "  ›  " + projectBrowser.category().label());
        refreshProjectBrowserVisibility();
        synchronizeProjectSelection();
    }

    /** Shows an intentional empty state or the active Grid/List presentation. */
    private void refreshProjectBrowserVisibility() {
        boolean empty = projectBrowser.visibleItems().isEmpty();
        assetBrowserEmpty.setManaged(empty);
        assetBrowserEmpty.setVisible(empty);
        assets.setManaged(!empty && !showingAssetGrid);
        assets.setVisible(!empty && !showingAssetGrid);
        assetGridScroll.setManaged(!empty && showingAssetGrid);
        assetGridScroll.setVisible(!empty && showingAssetGrid);
    }

    /** Creates one keyboard-focusable, selection-aware Project asset card. */
    private ToggleButton createAssetCard(EditorAssetItem item) {
        ToggleButton card = new ToggleButton();
        card.setGraphic(createAssetPresentation(item));
        card.setUserData(item);
        card.setToggleGroup(assetCardGroup);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setTooltip(new Tooltip(item.source().toString()));
        card.setOnAction(ignored -> selectProjectItem(item));
        card.getStyleClass().add("editor-asset-card");
        return card;
    }

    /** Synchronizes visual Project selections from the source-scoped browser identity. */
    private void synchronizeProjectSelection() {
        Optional<EditorAssetItem> selected = projectBrowser.selectedItem();
        Optional<EditorAssetItem> visibleSelection = selected.filter(assets.getItems()::contains);
        if (visibleSelection.isPresent()) {
            assets.getSelectionModel().select(visibleSelection.orElseThrow());
        } else {
            assets.getSelectionModel().clearSelection();
        }
        assetCardGroup.selectToggle(null);
        if (visibleSelection.isPresent()) {
            String identity = visibleSelection.orElseThrow().selection().identity();
            assetCardGroup.getToggles().stream()
                    .filter(toggle -> toggle.getUserData() instanceof EditorAssetItem item
                            && item.selection().identity().equals(identity))
                    .findFirst()
                    .ifPresent(assetCardGroup::selectToggle);
        }
    }

    /** Creates the shared author-facing content used by asset rows and cards. */
    private static VBox createAssetPresentation(EditorAssetItem item) {
        Label marker = new Label(assetMarker(item.kind()));
        marker.setTooltip(new Tooltip(item.kind().label()));
        marker.getStyleClass().add("editor-asset-marker");
        Label name = new Label(item.label());
        name.setMinWidth(0.0);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setPrefWidth(1.0);
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setTooltip(new Tooltip(item.label()));
        name.getStyleClass().add("editor-asset-name");
        HBox.setHgrow(name, Priority.ALWAYS);
        HBox heading = new HBox(6.0, marker, name);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setMaxWidth(Double.MAX_VALUE);
        Label kind = new Label(item.kind().label());
        kind.getStyleClass().add("editor-asset-kind");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label readOnly = new Label("R/O");
        readOnly.setTooltip(new Tooltip("The current editor opens project content read-only"));
        readOnly.getStyleClass().add("editor-asset-read-only");
        HBox metadata = new HBox(6.0, kind, spacer, readOnly);
        metadata.setAlignment(Pos.CENTER_LEFT);
        metadata.setMaxWidth(Double.MAX_VALUE);
        Label source = new Label(item.selection().inspector().source());
        source.setMinWidth(0.0);
        source.setMaxWidth(Double.MAX_VALUE);
        source.setPrefWidth(1.0);
        source.setTextOverrun(OverrunStyle.ELLIPSIS);
        source.setTooltip(new Tooltip(item.source().toString()));
        source.getStyleClass().add("editor-asset-source");
        VBox content = new VBox(4.0, heading, metadata, source);
        content.setMaxWidth(Double.MAX_VALUE);
        content.getStyleClass().add("editor-asset-presentation");
        return content;
    }

    /** Returns a compact non-decorative marker for one JScene3D asset kind. */
    private static String assetMarker(EditorAssetItem.Kind kind) {
        return switch (kind) {
            case WORLD_DEFINITION -> "W";
            case ENTITY_DEFINITION -> "E";
            case SOURCE_ASSET -> "S";
            case IMPORT_DEFINITION -> "I";
        };
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

    /** Converts one immutable hierarchy projection into JavaFX tree items. */
    private static TreeItem<EditorHierarchyNode> createTreeItem(EditorHierarchyNode node) {
        TreeItem<EditorHierarchyNode> item = new TreeItem<>(node);
        item.getChildren()
                .setAll(node.children().stream()
                        .map(EditorWorkspace::createTreeItem)
                        .toList());
        return item;
    }

    /** Renders one Project asset as a reusable, information-rich list row. */
    private static final class AssetListCell extends ListCell<EditorAssetItem> {
        /** Uses the editor's Project-row style. */
        private AssetListCell() {
            getStyleClass().add("editor-asset-list-cell");
        }

        @Override
        protected void updateItem(@Nullable EditorAssetItem item, boolean empty) {
            super.updateItem(item, empty);
            setText(null);
            setGraphic(empty || item == null ? null : createAssetPresentation(item));
        }
    }

    /** Renders the project root and JScene3D categories with compact markers and counts. */
    private static final class ProjectBrowserCell extends TreeCell<ProjectBrowserNode> {
        /** Uses the editor's Project-navigation style. */
        private ProjectBrowserCell() {
            getStyleClass().add("editor-project-tree-cell");
        }

        @Override
        protected void updateItem(@Nullable ProjectBrowserNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            Label marker = new Label(item.projectRoot() ? "P" : assetCategoryMarker(item.category()));
            marker.getStyleClass().add("editor-project-tree-marker");
            Label name = new Label(item.label());
            name.setMinWidth(0.0);
            name.setMaxWidth(Double.MAX_VALUE);
            name.setPrefWidth(1.0);
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            name.setTooltip(new Tooltip(item.label()));
            HBox.setHgrow(name, Priority.ALWAYS);
            Label count = new Label(Long.toString(item.count()));
            count.getStyleClass().add("editor-project-tree-count");
            HBox row = new HBox(7.0, marker, name, count);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            row.getStyleClass().add("editor-project-tree-row");
            setText(null);
            setAccessibleText(item.toString());
            setGraphic(row);
        }

        /** Returns the marker for a Project-browser category. */
        private static String assetCategoryMarker(EditorProjectBrowserModel.Category category) {
            return switch (category) {
                case WORLDS -> "W";
                case ENTITY_DEFINITIONS -> "E";
                case SOURCE_ASSETS -> "S";
                case IMPORTS -> "I";
                case ALL -> "P";
            };
        }
    }

    /** One typed location in the Project browser's navigation tree. */
    private record ProjectBrowserNode(
            String label, EditorProjectBrowserModel.Category category, long count, boolean projectRoot) {
        /** Creates the project root location. */
        private static ProjectBrowserNode project(String projectName, long count) {
            return new ProjectBrowserNode(projectName, EditorProjectBrowserModel.Category.ALL, count, true);
        }

        /** Creates one JScene3D category location. */
        private static ProjectBrowserNode category(EditorProjectBrowserModel.Category category, long count) {
            return new ProjectBrowserNode(category.label(), category, count, false);
        }

        @Override
        public String toString() {
            return projectRoot ? label : label + " (" + count + ')';
        }
    }
}
