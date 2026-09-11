/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorViewPlacement;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleBinaryOperator;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jspecify.annotations.Nullable;

/** Generic collapsible host for every view contributed to one panel container. */
public final class JavaFxPanelPart implements AutoCloseable {
    private static final double HEADER_HEIGHT = 34.0;

    private final ViewContainerId id;
    private final JavaFxViewRenderer renderer;
    private final double minimumHeight;
    private final double preferredHeight;
    private final DoubleBinaryOperator dividerPosition;
    private final VBox node = new VBox();
    private final HBox tabs = new HBox();
    private final StackPane actions = new StackPane();
    private final StackPane content = new StackPane();
    private final Button collapse = new Button("×");
    private final Map<ViewId, JavaFxRenderedView> renderedViews = new LinkedHashMap<>();
    private final Map<ViewId, Button> tabButtons = new LinkedHashMap<>();
    private @Nullable SplitPane splitPane;
    private SplitPane.@Nullable Divider observedDivider;
    private final ChangeListener<Number> dividerPositionListener;
    private final ListChangeListener<SplitPane.Divider> dividerListListener;
    private final EditorRegistration viewRegistration;
    private final EditorRegistration requestRegistration;
    private @Nullable ViewId selected;
    private boolean expanded = true;
    private boolean closed;
    private double expandedHeight;

    /**
     * Creates a panel which follows contributions for one workbench container.
     *
     * @param extensions active extension host
     * @param layout current workbench layout
     * @param id workbench view-container identity
     * @param icons icon renderer
     * @param minimumHeight minimum expanded height
     * @param preferredHeight preferred expanded height
     * @param dividerPosition function deriving the expanded divider position
     */
    public JavaFxPanelPart(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            ViewContainerId id,
            JavaFxIconRenderer icons,
            double minimumHeight,
            double preferredHeight,
            DoubleBinaryOperator dividerPosition) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        EditorWorkbenchLayout workbenchLayout = Objects.requireNonNull(layout, "layout");
        this.id = Objects.requireNonNull(id, "id");
        renderer = new JavaFxViewRenderer(host, Objects.requireNonNull(icons, "icons"));
        if (minimumHeight < HEADER_HEIGHT || preferredHeight < minimumHeight) {
            throw new IllegalArgumentException("panel heights must accommodate the header and preferred size");
        }
        this.minimumHeight = minimumHeight;
        this.preferredHeight = preferredHeight;
        this.dividerPosition = Objects.requireNonNull(dividerPosition, "dividerPosition");
        expandedHeight = preferredHeight;
        dividerPositionListener = (ignored, previous, current) -> rememberExpandedHeight(current.doubleValue());
        dividerListListener = ignored -> observeDivider();
        configureNode();
        viewRegistration = workbenchLayout.observeViews(this::showPlacements);
        requestRegistration = host.observeViewRequests(this::reveal);
    }

    /**
     * Returns the workbench-owned JavaFX node installed in layout.
     *
     * @return panel node
     */
    public VBox node() {
        return node;
    }

    /**
     * Attaches this lower panel to the vertical split pane which sizes it.
     *
     * @param owner vertical split pane that owns the panel
     */
    public void attach(SplitPane owner) {
        SplitPane candidate = Objects.requireNonNull(owner, "owner");
        if (splitPane != null) {
            throw new IllegalStateException("panel is already attached");
        }
        splitPane = candidate;
        candidate.getDividers().addListener(dividerListListener);
        observeDivider();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        requestRegistration.close();
        viewRegistration.close();
        SplitPane owner = splitPane;
        if (owner != null) {
            owner.getDividers().removeListener(dividerListListener);
        }
        replaceObservedDivider(null);
        closeRenderedViews();
        node.getChildren().clear();
    }

    private void configureNode() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        collapse.setAccessibleText("Collapse panel");
        collapse.setTooltip(new Tooltip("Collapse panel"));
        collapse.setOnAction(ignored -> collapse());
        collapse.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_CLOSE);
        HBox header = new HBox(tabs, spacer, actions, collapse);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMinHeight(HEADER_HEIGHT);
        header.setPrefHeight(HEADER_HEIGHT);
        header.setMaxHeight(HEADER_HEIGHT);
        header.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_HEADER);
        node.getChildren().setAll(header, content);
        VBox.setVgrow(content, Priority.ALWAYS);
        node.setMinHeight(minimumHeight);
        node.setPrefHeight(preferredHeight);
        node.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_PART);
    }

    private void showPlacements(List<EditorViewPlacement> placements) {
        List<EditorViewPlacement> matching = placements.stream()
                .filter(placement -> placement.container().equals(id))
                .toList();
        ViewId previousSelection = selected;
        closeRenderedViews();
        tabs.getChildren().clear();
        for (EditorViewPlacement placement : matching) {
            ViewId viewId = placement.view().id();
            JavaFxRenderedView rendered = renderer.render(placement.view());
            renderedViews.put(viewId, rendered);
            Button tab = new Button();
            tab.setGraphic(rendered.titleGraphic());
            tab.setAccessibleText(placement.view().title());
            tab.setOnAction(ignored -> select(viewId));
            tab.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_TAB);
            tabButtons.put(viewId, tab);
            tabs.getChildren().add(tab);
        }
        selected = renderedViews.containsKey(previousSelection)
                ? previousSelection
                : renderedViews.keySet().stream().findFirst().orElse(null);
        showSelected();
    }

    private void closeRenderedViews() {
        renderedViews.values().forEach(rendered -> rendered.close().run());
        renderedViews.clear();
        tabButtons.clear();
        content.getChildren().clear();
        actions.getChildren().clear();
    }

    private void select(ViewId view) {
        selected = view;
        expand();
    }

    private void reveal(ViewId view) {
        JavaFxRenderedView rendered = renderedViews.get(view);
        if (rendered == null) {
            return;
        }
        selected = view;
        expand();
        Platform.runLater(rendered.requestFocus());
    }

    private void expand() {
        expanded = true;
        node.setMinHeight(minimumHeight);
        node.setMaxHeight(Double.MAX_VALUE);
        node.setPrefHeight(expandedHeight);
        content.setManaged(true);
        content.setVisible(true);
        collapse.setManaged(true);
        collapse.setVisible(true);
        showSelected();
        Platform.runLater(() -> moveDivider(expandedHeight));
    }

    private void collapse() {
        if (!expanded) {
            return;
        }
        if (node.getHeight() >= minimumHeight) {
            expandedHeight = node.getHeight();
        }
        expanded = false;
        content.setManaged(false);
        content.setVisible(false);
        actions.setManaged(false);
        actions.setVisible(false);
        collapse.setManaged(false);
        collapse.setVisible(false);
        node.setMinHeight(HEADER_HEIGHT);
        node.setPrefHeight(HEADER_HEIGHT);
        node.setMaxHeight(HEADER_HEIGHT);
        updateTabs();
        Platform.runLater(() -> moveDivider(HEADER_HEIGHT));
    }

    private void showSelected() {
        JavaFxRenderedView rendered = selected == null ? null : renderedViews.get(selected);
        content.getChildren().setAll(rendered == null ? List.of() : List.of(rendered.node()));
        actions.getChildren()
                .setAll(
                        rendered == null
                                ? List.of()
                                : rendered.titleActions().map(List::of).orElseGet(List::of));
        boolean showActions = expanded && !actions.getChildren().isEmpty();
        actions.setManaged(showActions);
        actions.setVisible(showActions);
        updateTabs();
    }

    private void updateTabs() {
        tabButtons.values().forEach(tab -> tab.getStyleClass().remove(EditorStyleClasses.EDITOR_PANEL_TAB_ACTIVE));
        Button active = selected == null ? null : tabButtons.get(selected);
        if (active != null) {
            active.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_TAB_ACTIVE);
        }
    }

    private void moveDivider(double panelHeight) {
        SplitPane owner = splitPane;
        if (owner != null && owner.getHeight() > 0.0) {
            owner.setDividerPositions(dividerPosition.applyAsDouble(owner.getHeight(), panelHeight));
        }
    }

    private void observeDivider() {
        SplitPane owner = splitPane;
        SplitPane.Divider divider = owner == null || owner.getDividers().isEmpty()
                ? null
                : owner.getDividers().getFirst();
        replaceObservedDivider(divider);
    }

    private void rememberExpandedHeight(double dividerPositionValue) {
        SplitPane owner = splitPane;
        if (expanded && owner != null && owner.getHeight() > 0.0) {
            double height = owner.getHeight() * (1.0 - dividerPositionValue);
            if (height >= minimumHeight) {
                expandedHeight = height;
            }
        }
    }

    private void replaceObservedDivider(SplitPane.@Nullable Divider replacement) {
        if (observedDivider == replacement) {
            return;
        }
        if (observedDivider != null) {
            observedDivider.positionProperty().removeListener(dividerPositionListener);
        }
        observedDivider = replacement;
        if (replacement != null) {
            replacement.positionProperty().addListener(dividerPositionListener);
        }
    }
}
