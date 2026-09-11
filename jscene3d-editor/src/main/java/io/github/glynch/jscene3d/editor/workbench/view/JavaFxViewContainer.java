/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.ViewContainerId;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Fixed workbench container which renders registered logical views through JavaFX adapters. */
public final class JavaFxViewContainer implements AutoCloseable {
    private final JavaFxViewRenderer renderer;
    private final ViewContainerId id;
    private final VBox root = new VBox();
    private final Map<ViewId, JavaFxRenderedView> renderedViews = new LinkedHashMap<>();
    private final boolean showSingleHeading;
    private final EditorRegistration viewRegistration;
    private final EditorRegistration requestRegistration;

    /**
     * Creates a container which immediately observes contributions for one fixed location.
     *
     * @param extensions active extension host
     * @param id fixed workbench container identity
     * @param icons icon renderer
     */
    public JavaFxViewContainer(EditorExtensionHost extensions, ViewContainerId id, JavaFxIconRenderer icons) {
        this(extensions, id, true, icons);
    }

    /**
     * Creates a container with configurable heading ownership for an enclosing workbench surface.
     *
     * @param extensions active extension host
     * @param id fixed workbench container identity
     * @param showSingleHeading whether this container should render the title of a lone contribution
     * @param icons icon renderer
     */
    public JavaFxViewContainer(
            EditorExtensionHost extensions, ViewContainerId id, boolean showSingleHeading, JavaFxIconRenderer icons) {
        Objects.requireNonNull(extensions, "extensions");
        Objects.requireNonNull(icons, "icons");
        renderer = new JavaFxViewRenderer(extensions, icons);
        this.id = Objects.requireNonNull(id, "id");
        this.showSingleHeading = showSingleHeading;
        root.setSpacing(6.0);
        viewRegistration = extensions.observeViews(this::showContributions);
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

    @Override
    public void close() {
        requestRegistration.close();
        viewRegistration.close();
        renderedViews.values().forEach(rendered -> rendered.close().run());
        renderedViews.clear();
        root.getChildren().clear();
    }

    private void showContributions(List<EditorViewContribution> contributions) {
        List<EditorViewContribution> matching = contributions.stream()
                .filter(contribution -> contribution.container().equals(id))
                .toList();
        renderedViews.values().forEach(rendered -> rendered.close().run());
        renderedViews.clear();
        root.getChildren().clear();
        if (matching.size() == 1) {
            showSingle(matching.getFirst());
        } else if (!matching.isEmpty()) {
            showTabs(matching);
        }
    }

    private void showSingle(EditorViewContribution contribution) {
        JavaFxRenderedView rendered = renderer.render(contribution.view());
        renderedViews.put(contribution.view().id(), rendered);
        VBox.setVgrow(rendered.node(), Priority.ALWAYS);
        if (showSingleHeading) {
            Label heading = new Label(contribution.view().title());
            heading.getStyleClass().add(EditorStyleClasses.EDITOR_PANEL_HEADING);
            root.getChildren().setAll(heading, rendered.node());
        } else {
            root.getChildren().setAll(rendered.node());
        }
    }

    private void showTabs(List<EditorViewContribution> contributions) {
        TabPane tabs = new TabPane();
        for (EditorViewContribution contribution : contributions) {
            JavaFxRenderedView rendered = renderer.render(contribution.view());
            Tab tab = new Tab(contribution.view().title(), rendered.node());
            tab.setClosable(false);
            tab.setUserData(contribution.view().id());
            tabs.getTabs().add(tab);
            renderedViews.put(
                    contribution.view().id(),
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
        if (rendered != null) {
            rendered.requestFocus().run();
        }
    }
}
