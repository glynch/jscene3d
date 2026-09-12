/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.EditorProjectSession;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.workbench.configuration.JavaFxProjectSettingsPane;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorViewPlacement;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
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
import org.jspecify.annotations.Nullable;

/** Hosts Welcome, the project preview, and extension-provided editor-area views. */
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
    private final JavaFxIconRenderer icons;
    private final Runnable openProject;
    private @Nullable Tab welcomeTab;
    private @Nullable Tab settingsTab;

    /**
     * Creates an editor area around the scene preview.
     *
     * @param extensions active extension host
     * @param layout current workbench layout
     * @param icons icon renderer
     * @param previewTitleProperty observable scene-preview title
     * @param previewDirtyProperty observable scene-preview dirty state
     * @param previewContent rendered scene-preview content
     * @param openProject opens a project through the workbench command
     */
    public JavaFxEditorArea(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            JavaFxIconRenderer icons,
            ReadOnlyStringProperty previewTitleProperty,
            ReadOnlyBooleanProperty previewDirtyProperty,
            Node previewContent,
            Runnable openProject) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        this.icons = Objects.requireNonNull(icons, "icons");
        this.openProject = Objects.requireNonNull(openProject, "openProject");
        renderer = new JavaFxViewRenderer(host, this.icons);
        ReadOnlyStringProperty title = Objects.requireNonNull(previewTitleProperty, "previewTitleProperty");
        ReadOnlyBooleanProperty dirty = Objects.requireNonNull(previewDirtyProperty, "previewDirtyProperty");
        previewTitle.textProperty().bind(title);
        previewTitle
                .accessibleTextProperty()
                .bind(Bindings.createStringBinding(
                        () -> dirty.get() ? title.get() + ", modified" : title.get(), title, dirty));
        preview.textProperty()
                .bind(Bindings.createStringBinding(
                        () -> dirty.get() ? title.get() + ", modified" : title.get(), title, dirty));
        preview.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_GRAPHIC_ONLY);
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
        tabs.setAccessibleText("Editor tabs");
        tabs.setAccessibleHelp("Use Left and Right Arrow keys to switch editor tabs");
        tabs.getStyleClass().add(EditorStyleClasses.EDITOR_AREA_TABS);
        tabs.getTabs().add(preview);
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

    /** Opens or reveals the no-project Welcome editor. */
    public void showWelcome() {
        Tab current = welcomeTab;
        if (current == null) {
            Tab created = new Tab("Welcome", new JavaFxWelcomePane(icons, openProject));
            created.setClosable(true);
            created.setOnClosed(ignored -> welcomeTab = null);
            welcomeTab = created;
            tabs.getTabs().addFirst(created);
            current = created;
        }
        tabs.getSelectionModel().select(current);
    }

    /** Opens or reveals the project preview without closing an existing Welcome editor. */
    public void showProjectPreview() {
        if (!tabs.getTabs().contains(preview)) {
            tabs.getTabs().add(preview);
        }
        tabs.getSelectionModel().select(preview);
    }

    /** Removes project-owned editors and restores a Welcome editor when no project remains. */
    public void showEmptyWorkspace() {
        closeProjectSettings();
        tabs.getTabs().remove(preview);
        showWelcome();
    }

    /** Opens or reveals the generated settings editor for the current project. */
    public void showProjectSettings(EditorProjectSession session, Consumer<EditorMessage> messages) {
        Tab current = settingsTab;
        if (current == null) {
            JavaFxProjectSettingsPane settings = new JavaFxProjectSettingsPane(session, messages);
            Label title = new Label("Project Settings");
            title.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_TITLE);
            Tab created = new Tab();
            created.setGraphic(title);
            created.setContent(settings.node());
            created.setClosable(true);
            created.setOnClosed(ignored -> settingsTab = null);
            settingsTab = created;
            tabs.getTabs().add(created);
            current = created;
        }
        tabs.getSelectionModel().select(current);
    }

    /** Closes the project-owned settings editor when its project is replaced. */
    public void closeProjectSettings() {
        Tab current = settingsTab;
        settingsTab = null;
        if (current != null) {
            tabs.getTabs().remove(current);
        }
    }

    @Override
    public void close() {
        closeProjectSettings();
        welcomeTab = null;
        requestRegistration.close();
        viewRegistration.close();
        openViews.values().forEach(OpenView::close);
        openViews.clear();
        available.clear();
        previewTitle.textProperty().unbind();
        previewTitle.accessibleTextProperty().unbind();
        preview.textProperty().unbind();
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
