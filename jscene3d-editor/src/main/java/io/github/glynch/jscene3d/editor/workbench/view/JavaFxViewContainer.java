/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.activity.EditorActivitySelectionState;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorViewPlacement;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Workbench container which renders the logical views currently placed within it. */
public final class JavaFxViewContainer implements AutoCloseable {
    private final JavaFxViewRenderer renderer;
    private final ViewContainerId id;
    private final VBox root = new VBox();
    private final Map<ViewId, JavaFxRenderedView> renderedViews = new LinkedHashMap<>();
    private final boolean showSingleHeading;
    private final EditorRegistration viewRegistration;
    private final EditorRegistration requestRegistration;
    private List<EditorViewPlacement> placements = List.of();
    private Optional<ViewId> shownView = Optional.empty();
    private Optional<EditorActivitySelectionState> activitySelection = Optional.empty();

    /**
     * Creates a container which immediately observes current placements for one location.
     *
     * @param extensions active extension host
     * @param layout current workbench layout
     * @param id workbench container identity
     * @param icons icon renderer
     */
    public JavaFxViewContainer(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            ViewContainerId id,
            JavaFxIconRenderer icons) {
        this(extensions, layout, id, true, icons);
    }

    /**
     * Creates a container with configurable heading ownership for an enclosing workbench surface.
     *
     * @param extensions active extension host
     * @param layout current workbench layout
     * @param id workbench container identity
     * @param showSingleHeading whether this container should render the title of a lone contribution
     * @param icons icon renderer
     */
    public JavaFxViewContainer(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            ViewContainerId id,
            boolean showSingleHeading,
            JavaFxIconRenderer icons) {
        Objects.requireNonNull(extensions, "extensions");
        Objects.requireNonNull(icons, "icons");
        renderer = new JavaFxViewRenderer(extensions, icons);
        EditorWorkbenchLayout workbenchLayout = Objects.requireNonNull(layout, "layout");
        this.id = Objects.requireNonNull(id, "id");
        this.showSingleHeading = showSingleHeading;
        root.setSpacing(6.0);
        viewRegistration = workbenchLayout.observeViews(this::showPlacements);
        requestRegistration = extensions.observeViewRequests(this::reveal);
    }

    /**
     * Returns the workbench-owned JavaFX node for layout.
     *
     * @return container node
     */
    public VBox node() {
        return root;
    }

    /**
     * Prefers one view when it is currently placed in this container.
     *
     * <p>Activity Bar selections use this to replace the visible primary-side-bar content without changing the
     * contributed or customized placement.
     *
     * @param view preferred view identity
     */
    public void showOnly(ViewId view) {
        shownView = Optional.of(Objects.requireNonNull(view, "view"));
        renderPlacements();
    }

    /**
     * Replaces Activity Bar-owned content with the selected activity container while retaining ordinary moved views.
     *
     * @param selection selected activity and complete activity-owned view membership
     */
    public void showActivity(EditorActivitySelectionState selection) {
        activitySelection = Optional.of(Objects.requireNonNull(selection, "selection"));
        shownView = Optional.empty();
        renderPlacements();
    }

    @Override
    public void close() {
        requestRegistration.close();
        viewRegistration.close();
        renderedViews.values().forEach(rendered -> rendered.close().run());
        renderedViews.clear();
        root.getChildren().clear();
    }

    private void showPlacements(List<EditorViewPlacement> placements) {
        this.placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
        renderPlacements();
    }

    private void renderPlacements() {
        List<EditorViewPlacement> matching = placements.stream()
                .filter(placement -> placement.container().equals(id))
                .toList();
        matching = filterActivityViews(matching);
        List<EditorViewPlacement> available = matching;
        Optional<EditorViewPlacement> preferred = shownView.flatMap(view -> available.stream()
                .filter(placement -> placement.view().id().equals(view))
                .findFirst());
        if (preferred.isPresent()) {
            matching = List.of(preferred.orElseThrow());
        }
        renderedViews.values().forEach(rendered -> rendered.close().run());
        renderedViews.clear();
        root.getChildren().clear();
        if (matching.size() == 1) {
            showSingle(matching.getFirst());
        } else if (!matching.isEmpty()) {
            showTabs(matching);
        }
    }

    private List<EditorViewPlacement> filterActivityViews(List<EditorViewPlacement> matching) {
        return activitySelection
                .map(selection -> matching.stream()
                        .filter(placement -> selection.includes(placement.view().id()))
                        .toList())
                .orElse(matching);
    }

    private void showSingle(EditorViewPlacement placement) {
        JavaFxRenderedView rendered = renderer.render(placement.view());
        renderedViews.put(placement.view().id(), rendered);
        VBox.setVgrow(rendered.node(), Priority.ALWAYS);
        if (showSingleHeading) {
            Label heading = new Label(placement.view().title());
            heading.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_HEADING);
            root.getChildren().setAll(heading, rendered.node());
        } else {
            root.getChildren().setAll(rendered.node());
        }
    }

    private void showTabs(List<EditorViewPlacement> placements) {
        TabPane tabs = new TabPane();
        for (EditorViewPlacement placement : placements) {
            JavaFxRenderedView rendered = renderer.render(placement.view());
            Tab tab = new Tab(placement.view().title(), rendered.node());
            tab.setClosable(false);
            tab.setUserData(placement.view().id());
            tabs.getTabs().add(tab);
            renderedViews.put(
                    placement.view().id(),
                    new JavaFxRenderedView(
                            rendered.node(), rendered.titleGraphic(), rendered.titleActions(), rendered.close(), () -> {
                                tabs.getSelectionModel().select(tab);
                                rendered.requestFocus().run();
                            }));
        }
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.getChildren().setAll(tabs);
    }

    private void reveal(ViewId requested) {
        JavaFxRenderedView rendered = renderedViews.get(requested);
        if (rendered == null
                && placements.stream()
                        .anyMatch(placement -> placement.container().equals(id)
                                && placement.view().id().equals(requested))) {
            showOnly(requested);
            rendered = renderedViews.get(requested);
        }
        if (rendered != null) {
            rendered.requestFocus().run();
        }
    }
}
