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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.Tab;

/** Opens extension-contributed views in the central editor area. */
public final class JavaFxContributedEditors implements AutoCloseable {
    private final JavaFxEditorArea area;
    private final JavaFxViewRenderer renderer;
    private final Map<ViewId, EditorViewPlacement> available = new LinkedHashMap<>();
    private final Map<ViewId, OpenView> openViews = new LinkedHashMap<>();
    private final EditorRegistration viewRegistration;
    private final EditorRegistration requestRegistration;

    /** Connects editor-area view contributions to the generic tab surface. */
    public JavaFxContributedEditors(
            JavaFxEditorArea area,
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            JavaFxIconRenderer icons) {
        this.area = Objects.requireNonNull(area, "area");
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        renderer = new JavaFxViewRenderer(host, Objects.requireNonNull(icons, "icons"));
        viewRegistration = Objects.requireNonNull(layout, "layout").observeViews(this::showPlacements);
        requestRegistration = host.observeViewRequests(this::reveal);
    }

    private void showPlacements(List<EditorViewPlacement> placements) {
        available.clear();
        placements.stream()
                .filter(placement -> placement.container().equals(EditorViewContainers.EDITOR_AREA))
                .forEach(placement -> available.put(placement.view().id(), placement));
        openViews.keySet().stream()
                .filter(view -> !available.containsKey(view))
                .toList()
                .forEach(this::closeView);
    }

    private void reveal(ViewId view) {
        EditorViewPlacement placement = available.get(view);
        if (placement == null) {
            return;
        }
        OpenView opened = openViews.computeIfAbsent(view, ignored -> openView(placement));
        area.select(opened.tab());
    }

    private OpenView openView(EditorViewPlacement placement) {
        JavaFxRenderedView rendered = renderer.render(placement.view());
        Tab tab = new Tab();
        tab.setGraphic(rendered.titleGraphic());
        tab.setContent(rendered.node());
        tab.setClosable(true);
        tab.setOnClosed(ignored -> closeView(placement.view().id()));
        OpenView result = new OpenView(tab, rendered);
        area.add(tab, rendered.requestFocus());
        return result;
    }

    private void closeView(ViewId view) {
        OpenView removed = openViews.remove(view);
        if (removed != null) {
            area.remove(removed.tab());
            removed.close();
        }
    }

    @Override
    public void close() {
        requestRegistration.close();
        viewRegistration.close();
        List<OpenView> views = List.copyOf(openViews.values());
        views.stream().map(OpenView::tab).forEach(area::remove);
        views.forEach(OpenView::close);
        openViews.clear();
        available.clear();
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
