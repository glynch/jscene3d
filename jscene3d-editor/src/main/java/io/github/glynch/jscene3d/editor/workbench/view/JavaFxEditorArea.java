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
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Hosts the permanent scene preview and extension-provided editor-area views. */
public final class JavaFxEditorArea implements AutoCloseable {
    private final JavaFxViewRenderer renderer;
    private final TabPane tabs = new TabPane();
    private final Tab preview = new Tab();
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
     * @param previewTitle observable scene-preview title
     * @param previewContent rendered scene-preview content
     */
    public JavaFxEditorArea(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            JavaFxIconRenderer icons,
            StringProperty previewTitle,
            Node previewContent) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        renderer = new JavaFxViewRenderer(host, Objects.requireNonNull(icons, "icons"));
        preview.textProperty().bind(Objects.requireNonNull(previewTitle, "previewTitle"));
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
        preview.textProperty().unbind();
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
