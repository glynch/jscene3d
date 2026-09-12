/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** Hosts the permanent scene preview and extension-provided editor-area views. */
public final class JavaFxEditorArea implements AutoCloseable {
    private final JavaFxViewRenderer renderer;
    private final TabPane tabs = new TabPane();
    private final Tab preview = new Tab();
    private final Label previewTitle = new Label();
    private final Region previewDirtyIndicator = new Region();
    private final Map<ViewId, EditorViewPlacement> available = new LinkedHashMap<>();
    private final Map<ViewId, OpenView> openViews = new LinkedHashMap<>();
    private final EditorRegistration viewRegistration;
    private final EditorRegistration requestRegistration;

    /**
     * Creates an editor area around the scene preview.
     *
     * @param extensions active extension host
     * @param layout current workbench layout
     * @param icons icon renderer
     * @param previewTitleProperty observable scene-preview title
     * @param previewDirtyProperty observable scene-preview dirty state
     * @param previewContent rendered scene-preview content
     */
    public JavaFxEditorArea(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            JavaFxIconRenderer icons,
            ReadOnlyStringProperty previewTitleProperty,
            ReadOnlyBooleanProperty previewDirtyProperty,
            Node previewContent) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        renderer = new JavaFxViewRenderer(host, Objects.requireNonNull(icons, "icons"));
        ReadOnlyStringProperty title = Objects.requireNonNull(previewTitleProperty, "previewTitleProperty");
        ReadOnlyBooleanProperty dirty = Objects.requireNonNull(previewDirtyProperty, "previewDirtyProperty");
        previewTitle.textProperty().bind(title);
        previewTitle
                .accessibleTextProperty()
                .bind(Bindings.createStringBinding(
                        () -> dirty.get() ? title.get() + ", modified" : title.get(), title, dirty));
        previewTitle.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_TITLE);
        previewDirtyIndicator.visibleProperty().bind(dirty);
        previewDirtyIndicator.managedProperty().bind(dirty);
        previewDirtyIndicator.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_DIRTY);
        HBox previewGraphic = new HBox(7.0, previewTitle, previewDirtyIndicator);
        previewGraphic.setAlignment(Pos.CENTER_LEFT);
        previewGraphic.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB);
        preview.setGraphic(previewGraphic);
        preview.setContent(Objects.requireNonNull(previewContent, "previewContent"));
        preview.setClosable(false);
        tabs.getTabs().add(preview);
        tabs.getStyleClass().add(EditorStyleClasses.EDITOR_AREA_TABS);
        viewRegistration = Objects.requireNonNull(layout, "layout").observeViews(this::showPlacements);
        requestRegistration = host.observeViewRequests(this::reveal);
    }

    /**
     * Returns the workbench-owned JavaFX node.
     *
     * @return editor-area node
     */
    public TabPane node() {
        return tabs;
    }

    @Override
    public void close() {
        requestRegistration.close();
        viewRegistration.close();
        openViews.values().forEach(OpenView::close);
        openViews.clear();
        available.clear();
        previewTitle.textProperty().unbind();
        previewTitle.accessibleTextProperty().unbind();
        previewDirtyIndicator.visibleProperty().unbind();
        previewDirtyIndicator.managedProperty().unbind();
        tabs.getTabs().clear();
    }

    private void showPlacements(List<EditorViewPlacement> placements) {
        available.clear();
        placements.stream()
                .filter(placement -> placement.container().equals(EditorViewContainers.EDITOR_AREA))
                .forEach(placement -> available.put(placement.view().id(), placement));
        List<ViewId> removed = openViews.keySet().stream()
                .filter(view -> !available.containsKey(view))
                .toList();
        removed.forEach(this::closeView);
    }

    private void reveal(ViewId view) {
        EditorViewPlacement placement = available.get(view);
        if (placement == null) {
            return;
        }
        OpenView opened = openViews.computeIfAbsent(view, ignored -> openView(placement));
        tabs.getSelectionModel().select(opened.tab());
        opened.rendered().requestFocus().run();
    }

    private OpenView openView(EditorViewPlacement placement) {
        JavaFxRenderedView rendered = renderer.render(placement.view());
        Tab tab = new Tab();
        tab.setGraphic(rendered.titleGraphic());
        tab.setContent(rendered.node());
        tab.setClosable(true);
        tab.setOnClosed(ignored -> closeView(placement.view().id()));
        OpenView result = new OpenView(tab, rendered);
        tabs.getTabs().add(tab);
        return result;
    }

    private void closeView(ViewId view) {
        OpenView removed = openViews.remove(view);
        if (removed != null) {
            tabs.getTabs().remove(removed.tab());
            removed.close();
        }
    }

    private record OpenView(Tab tab, JavaFxRenderedView rendered) {
        private OpenView {
            Objects.requireNonNull(tab, "tab");
            Objects.requireNonNull(rendered, "rendered");
        }

        private void close() {
            rendered.close().run();
        }
    }
}
