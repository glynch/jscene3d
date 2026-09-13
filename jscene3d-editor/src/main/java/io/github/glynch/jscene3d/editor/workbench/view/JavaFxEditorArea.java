/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.EditorProjectSession;
import io.github.glynch.jscene3d.editor.builtin.text.EditorTextFileWorkingCopy;
import io.github.glynch.jscene3d.editor.builtin.text.JavaFxMonacoEditor;
import io.github.glynch.jscene3d.editor.file.EditorFileKind;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.configuration.JavaFxProjectSettingsPane;
import io.github.glynch.jscene3d.editor.workbench.dialog.EditorWindowCloseGuard;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorViewPlacement;
import io.github.glynch.jscene3d.editor.workbench.layout.EditorWorkbenchLayout;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorWorkingCopyRegistry;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.jspecify.annotations.Nullable;

/** Hosts Welcome, the project preview, and extension-provided editor-area views. */
public final class JavaFxEditorArea implements AutoCloseable {
    /** Observable presentation state for the permanent scene-preview tab. */
    public record PreviewDescriptor(ReadOnlyStringProperty title, ReadOnlyBooleanProperty dirty, Node content) {
        /** Creates a validated preview descriptor. */
        public PreviewDescriptor {
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(dirty, "dirty");
            Objects.requireNonNull(content, "content");
        }
    }

    private final JavaFxViewRenderer renderer;
    private final TabPane tabs = new TabPane();
    private final Tab preview = new Tab();
    private final Label previewTitle = new Label();
    private final Region previewDirtyIndicator = new Region();
    private final Map<ViewId, EditorViewPlacement> available = new LinkedHashMap<>();
    private final Map<ViewId, OpenView> openViews = new LinkedHashMap<>();
    private final Map<Path, OpenFile> openFiles = new LinkedHashMap<>();
    private final EditorWorkingCopyRegistry fileWorkingCopies = new EditorWorkingCopyRegistry();
    private final EditorRegistration viewRegistration;
    private final EditorRegistration requestRegistration;
    private final EditorRegistration fileRequestRegistration;
    private final JavaFxIconRenderer icons;
    private final EditorExtensionHost extensions;
    private final EditorWindowCloseGuard closeGuard;
    private final Runnable openProject;
    private final Runnable fileStateChanged;
    private @Nullable Tab welcomeTab;
    private @Nullable Tab settingsTab;

    /**
     * Creates an editor area around the scene preview.
     *
     * @param extensions active extension host
     * @param layout current workbench layout
     * @param icons icon renderer
     * @param previewDescriptor scene-preview presentation
     * @param openProject opens a project through the workbench command
     * @param fileStateChanged refreshes workbench command state after editor-file changes
     */
    public JavaFxEditorArea(
            EditorExtensionHost extensions,
            EditorWorkbenchLayout layout,
            JavaFxIconRenderer icons,
            PreviewDescriptor previewDescriptor,
            Runnable openProject,
            Runnable fileStateChanged) {
        EditorExtensionHost host = Objects.requireNonNull(extensions, "extensions");
        this.extensions = host;
        this.icons = Objects.requireNonNull(icons, "icons");
        this.openProject = Objects.requireNonNull(openProject, "openProject");
        this.fileStateChanged = Objects.requireNonNull(fileStateChanged, "fileStateChanged");
        closeGuard = new EditorWindowCloseGuard(host::showDialog);
        renderer = new JavaFxViewRenderer(host, this.icons);
        PreviewDescriptor previewPresentation = Objects.requireNonNull(previewDescriptor, "previewDescriptor");
        ReadOnlyStringProperty title = previewPresentation.title();
        ReadOnlyBooleanProperty dirty = previewPresentation.dirty();
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
        preview.setContent(previewPresentation.content());
        preview.setClosable(false);
        tabs.setAccessibleText("Editor tabs");
        tabs.setAccessibleHelp("Use Left and Right Arrow keys to switch editor tabs");
        tabs.getStyleClass().add(EditorStyleClasses.EDITOR_AREA_TABS);
        tabs.getTabs().add(preview);
        tabs.getSelectionModel().selectedItemProperty().addListener((ignored, previous, selected) -> {
            this.fileStateChanged.run();
            selectedFile().ifPresent(OpenFile::requestFocus);
        });
        viewRegistration = Objects.requireNonNull(layout, "layout").observeViews(this::showPlacements);
        requestRegistration = host.observeViewRequests(this::reveal);
        fileRequestRegistration = host.observeFileRequests(this::openFile);
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
        fileRequestRegistration.close();
        requestRegistration.close();
        viewRegistration.close();
        openFiles.values().forEach(OpenFile::close);
        openFiles.clear();
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

    /** Returns whether any source editor contains unsaved content. */
    public boolean hasDirtyFiles() {
        return fileWorkingCopies.hasDirty();
    }

    /** Returns the number of dirty source-editor resources. */
    public int dirtyFileCount() {
        return fileWorkingCopies.dirtyCount();
    }

    /** Returns whether a workspace file editor is currently selected. */
    public boolean hasSelectedFile() {
        return selectedFile().isPresent();
    }

    /** Returns whether the selected workspace file has unsaved content. */
    public boolean isSelectedFileDirty() {
        return selectedFile().map(OpenFile::isDirty).orElse(false);
    }

    /** Saves the selected file when dirty and reports whether persistence succeeded. */
    public boolean saveSelectedFile() {
        return selectedFile().filter(OpenFile::isDirty).map(OpenFile::save).orElse(true);
    }

    /** Saves every dirty source file and reports whether every persistence operation succeeded. */
    public boolean saveAllFiles() {
        return openFiles.values().stream().filter(OpenFile::isDirty).allMatch(OpenFile::save);
    }

    /** Applies undo in the selected source editor when available. */
    public void undoSelectedFile() {
        selectedFile().ifPresent(OpenFile::undo);
    }

    /** Applies redo in the selected source editor when available. */
    public void redoSelectedFile() {
        selectedFile().ifPresent(OpenFile::redo);
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

    private void openFile(URI resource) {
        Path path;
        try {
            path = Path.of(Objects.requireNonNull(resource, "resource"))
                    .toAbsolutePath()
                    .normalize();
        } catch (IllegalArgumentException exception) {
            showFileError("Only local workspace files can be opened", exception);
            return;
        }
        OpenFile existing = openFiles.get(path);
        if (existing != null) {
            tabs.getSelectionModel().select(existing.tab());
            existing.requestFocus();
            return;
        }
        if (!Files.isRegularFile(path)) {
            showFileError("Unable to open " + path.getFileName(), new IOException("Not a regular file"));
            return;
        }
        try {
            OpenFile opened = createFileEditor(path, extensions.resolveFileType(path.toUri()));
            openFiles.put(path, opened);
            tabs.getTabs().add(opened.tab());
            tabs.getSelectionModel().select(opened.tab());
            opened.requestFocus();
            fileStateChanged.run();
        } catch (IOException | RuntimeException exception) {
            showFileError("Unable to open " + path.getFileName(), exception);
        }
    }

    private OpenFile createFileEditor(Path path, Optional<EditorFileType> resolved) throws IOException {
        if (resolved.map(EditorFileType::kind)
                .filter(EditorFileKind.IMAGE::equals)
                .isPresent()) {
            EditorFileType type = resolved.orElseThrow();
            return createImageEditor(path, type.icon());
        }
        EditorTextFileWorkingCopy workingCopy = EditorTextFileWorkingCopy.load(path);
        EditorFileType type = resolved.filter(candidate -> candidate.kind() == EditorFileKind.TEXT)
                .orElse(null);
        var language =
                type == null ? EditorLanguages.PLAIN_TEXT : type.language().orElseThrow();
        EditorIcon icon = type == null ? new EditorIcon(EditorIcons.TEXT_FILE, "Text file") : type.icon();
        EditorRegistration registration = fileWorkingCopies.register(workingCopy);
        JavaFxMonacoEditor editor = new JavaFxMonacoEditor(
                workingCopy,
                language,
                fileStateChanged,
                message -> showFileError("Unable to load " + path.getFileName(), new IOException(message)),
                exception -> showSaveError(path, exception));
        return createOpenFile(path, icon, editor.node(), editor::requestFocus, workingCopy, editor, registration);
    }

    private OpenFile createImageEditor(Path path, EditorIcon icon) {
        ImageView image = new ImageView(new Image(path.toUri().toString(), true));
        image.setPreserveRatio(true);
        image.setSmooth(true);
        StackPane imageCanvas = new StackPane(image);
        ScrollPane scroll = new ScrollPane(imageCanvas);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        image.fitWidthProperty().bind(scroll.widthProperty().subtract(48.0));
        image.fitHeightProperty().bind(scroll.heightProperty().subtract(48.0));
        return createOpenFile(path, icon, scroll, scroll::requestFocus, null, () -> {}, () -> {});
    }

    private OpenFile createOpenFile(
            Path path,
            EditorIcon icon,
            Node content,
            Runnable requestFocus,
            @Nullable EditorTextFileWorkingCopy workingCopy,
            AutoCloseable editor,
            EditorRegistration workingCopyRegistration) {
        Label title = new Label(path.getFileName().toString());
        title.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_TITLE);
        Region dirty = new Region();
        dirty.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_DIRTY);
        if (workingCopy == null) {
            dirty.setVisible(false);
            dirty.setManaged(false);
        } else {
            dirty.setVisible(workingCopy.isDirty());
            dirty.setManaged(workingCopy.isDirty());
        }
        HBox graphic = new HBox(7.0, icons.create(icon), title, dirty);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB);
        Tab tab = new Tab();
        tab.setText(path.getFileName().toString());
        tab.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_GRAPHIC_ONLY);
        tab.setGraphic(graphic);
        tab.setContent(content);
        tab.setClosable(true);
        EditorRegistration dirtyPresentationRegistration = workingCopy == null
                ? () -> {}
                : workingCopy.onDidChangeDirty().subscribe(ignored -> {
                    dirty.setVisible(workingCopy.isDirty());
                    dirty.setManaged(workingCopy.isDirty());
                });
        OpenFile opened = new OpenFile(
                path, tab, requestFocus, workingCopy, editor, workingCopyRegistration, dirtyPresentationRegistration);
        tab.setOnCloseRequest(event -> {
            if (!confirmClose(opened)) {
                event.consume();
            }
        });
        tab.setOnClosed(ignored -> removeFile(opened));
        return opened;
    }

    private boolean confirmClose(OpenFile file) {
        return !file.isDirty()
                || closeGuard.confirmClose(file.path().getFileName().toString(), 1, file::save);
    }

    private void removeFile(OpenFile file) {
        if (openFiles.remove(file.path(), file)) {
            file.close();
            fileStateChanged.run();
        }
    }

    private Optional<OpenFile> selectedFile() {
        Tab selected = tabs.getSelectionModel().getSelectedItem();
        return openFiles.values().stream()
                .filter(file -> file.tab() == selected)
                .findFirst();
    }

    private void showSaveError(Path path, IOException exception) {
        showFileError("Unable to save " + path.getFileName(), exception);
    }

    private void showFileError(String heading, Exception exception) {
        extensions.showMessage(new EditorMessage(
                EditorMessageSeverity.ERROR,
                heading + ": " + Objects.requireNonNullElse(exception.getMessage(), exception.toString())));
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

    private final class OpenFile {
        private final Path path;
        private final Tab tab;
        private final Runnable focus;
        private final @Nullable EditorTextFileWorkingCopy workingCopy;
        private final AutoCloseable editor;
        private final EditorRegistration workingCopyRegistration;
        private final EditorRegistration dirtyPresentationRegistration;
        private boolean closed;

        private OpenFile(
                Path path,
                Tab tab,
                Runnable focus,
                @Nullable EditorTextFileWorkingCopy workingCopy,
                AutoCloseable editor,
                EditorRegistration workingCopyRegistration,
                EditorRegistration dirtyPresentationRegistration) {
            this.path = path;
            this.tab = tab;
            this.focus = focus;
            this.workingCopy = workingCopy;
            this.editor = editor;
            this.workingCopyRegistration = workingCopyRegistration;
            this.dirtyPresentationRegistration = dirtyPresentationRegistration;
        }

        private Path path() {
            return path;
        }

        private Tab tab() {
            return tab;
        }

        private boolean isDirty() {
            return workingCopy != null && workingCopy.isDirty();
        }

        private boolean save() {
            if (workingCopy == null) {
                return true;
            }
            try {
                workingCopy.save();
                fileStateChanged.run();
                return true;
            } catch (IOException exception) {
                showSaveError(path, exception);
                return false;
            }
        }

        private void requestFocus() {
            focus.run();
        }

        private void undo() {
            if (editor instanceof JavaFxMonacoEditor sourceEditor) {
                sourceEditor.undo();
            }
        }

        private void redo() {
            if (editor instanceof JavaFxMonacoEditor sourceEditor) {
                sourceEditor.redo();
            }
        }

        private void close() {
            if (closed) {
                return;
            }
            closed = true;
            dirtyPresentationRegistration.close();
            workingCopyRegistration.close();
            try {
                editor.close();
            } catch (Exception exception) {
                throw new IllegalStateException("Unable to close file editor for " + path, exception);
            }
        }
    }
}
