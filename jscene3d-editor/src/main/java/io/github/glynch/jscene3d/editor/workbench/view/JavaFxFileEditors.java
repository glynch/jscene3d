/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.builtin.text.EditorTextFileWorkingCopy;
import io.github.glynch.jscene3d.editor.builtin.text.JavaFxMonacoEditor;
import io.github.glynch.jscene3d.editor.file.EditorFileKind;
import io.github.glynch.jscene3d.editor.file.EditorFileType;
import io.github.glynch.jscene3d.editor.file.EditorLanguages;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.view.EditorIcons;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.dialog.EditorWindowCloseGuard;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorExtensionHost;
import io.github.glynch.jscene3d.editor.workbench.extension.EditorFileOpenRequest;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.workingcopy.EditorWorkingCopyRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.jspecify.annotations.Nullable;

/** Opens workspace files and coordinates their tabs and working copies. */
public final class JavaFxFileEditors implements AutoCloseable {
    private final JavaFxEditorArea area;
    private final EditorExtensionHost extensions;
    private final JavaFxIconRenderer icons;
    private final Runnable stateChanged;
    private final EditorWindowCloseGuard closeGuard;
    private final Map<Path, JavaFxOpenFile> openFiles = new LinkedHashMap<>();
    private final EditorWorkingCopyRegistry workingCopies = new EditorWorkingCopyRegistry();
    private final EditorRegistration requestRegistration;
    private @Nullable JavaFxOpenFile previewFile;

    /** Connects file-open requests to file-type-specific editor tabs. */
    public JavaFxFileEditors(
            JavaFxEditorArea area, EditorExtensionHost extensions, JavaFxIconRenderer icons, Runnable stateChanged) {
        this.area = Objects.requireNonNull(area, "area");
        this.extensions = Objects.requireNonNull(extensions, "extensions");
        this.icons = Objects.requireNonNull(icons, "icons");
        this.stateChanged = Objects.requireNonNull(stateChanged, "stateChanged");
        closeGuard = new EditorWindowCloseGuard(extensions::showDialog);
        requestRegistration = extensions.observeFileRequests(this::openFile);
    }

    /** Returns the number of dirty source-editor resources. */
    public int dirtyCount() {
        return workingCopies.dirtyCount();
    }

    /** Returns whether a workspace file editor is selected. */
    public boolean hasSelectedFile() {
        return selectedFile().isPresent();
    }

    /** Returns whether the selected workspace file has unsaved content. */
    public boolean isSelectedFileDirty() {
        return selectedFile().map(JavaFxOpenFile::isDirty).orElse(false);
    }

    /** Saves the selected file when dirty. */
    public boolean saveSelectedFile() {
        return selectedFile()
                .filter(JavaFxOpenFile::isDirty)
                .map(JavaFxOpenFile::save)
                .orElse(true);
    }

    /** Saves every dirty source file. */
    public boolean saveAll() {
        return openFiles.values().stream().filter(JavaFxOpenFile::isDirty).allMatch(JavaFxOpenFile::save);
    }

    /** Applies undo in the selected source editor when available. */
    public void undoSelectedFile() {
        selectedFile().ifPresent(JavaFxOpenFile::undo);
    }

    /** Applies redo in the selected source editor when available. */
    public void redoSelectedFile() {
        selectedFile().ifPresent(JavaFxOpenFile::redo);
    }

    private void openFile(EditorFileOpenRequest request) {
        Path path;
        try {
            path = Path.of(Objects.requireNonNull(request, "request").resource())
                    .toAbsolutePath()
                    .normalize();
        } catch (IllegalArgumentException exception) {
            showFileError("Only local workspace files can be opened", exception);
            return;
        }
        JavaFxOpenFile existing = openFiles.get(path);
        if (existing != null) {
            reveal(existing, request.disposition());
            return;
        }
        if (!Files.isRegularFile(path)) {
            showFileError("Unable to open " + path.getFileName(), new IOException("Not a regular file"));
            return;
        }
        try {
            boolean pinned = request.disposition() == EditorFileOpenRequest.Disposition.PINNED;
            JavaFxOpenFile opened = createFileEditor(path, extensions.resolveFileType(path.toUri()), pinned);
            if (!pinned) {
                discardPreview();
                previewFile = opened;
            }
            openFiles.put(path, opened);
            area.add(opened.tab(), opened::requestFocus);
            reveal(opened, request.disposition());
            stateChanged.run();
        } catch (IOException | RuntimeException exception) {
            showFileError("Unable to open " + path.getFileName(), exception);
        }
    }

    private JavaFxOpenFile createFileEditor(Path path, Optional<EditorFileType> resolved, boolean pinned)
            throws IOException {
        if (resolved.map(EditorFileType::kind)
                .filter(EditorFileKind.IMAGE::equals)
                .isPresent()) {
            return createImageEditor(path, resolved.orElseThrow().icon(), pinned);
        }
        EditorTextFileWorkingCopy workingCopy = EditorTextFileWorkingCopy.load(path);
        EditorFileType type = resolved.filter(candidate -> candidate.kind() == EditorFileKind.TEXT)
                .orElse(null);
        var language =
                type == null ? EditorLanguages.PLAIN_TEXT : type.language().orElseThrow();
        EditorIcon icon = type == null ? new EditorIcon(EditorIcons.TEXT_FILE, "Text file") : type.icon();
        EditorRegistration registration = workingCopies.register(workingCopy);
        JavaFxMonacoEditor editor = new JavaFxMonacoEditor(
                workingCopy,
                language,
                extensions.colorThemes(),
                stateChanged,
                message -> showFileError("Unable to load " + path.getFileName(), new IOException(message)),
                exception -> showFileError("Unable to save " + path.getFileName(), exception));
        JavaFxFileEditorContent content =
                new JavaFxFileEditorContent(editor.node(), editor::requestFocus, editor::undo, editor::redo, editor);
        return createOpenFile(path, icon, content, workingCopy, registration, pinned);
    }

    private JavaFxOpenFile createImageEditor(Path path, EditorIcon icon, boolean pinned) {
        ImageView image = new ImageView(new Image(path.toUri().toString(), true));
        image.setPreserveRatio(true);
        image.setSmooth(true);
        ScrollPane scroll = new ScrollPane(new StackPane(image));
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(true);
        image.fitWidthProperty().bind(scroll.widthProperty().subtract(48.0));
        image.fitHeightProperty().bind(scroll.heightProperty().subtract(48.0));
        JavaFxFileEditorContent content =
                new JavaFxFileEditorContent(scroll, scroll::requestFocus, () -> {}, () -> {}, () -> {});
        return createOpenFile(path, icon, content, null, () -> {}, pinned);
    }

    private JavaFxOpenFile createOpenFile(
            Path path,
            EditorIcon icon,
            JavaFxFileEditorContent content,
            @Nullable EditorTextFileWorkingCopy workingCopy,
            EditorRegistration registration,
            boolean pinned) {
        return new JavaFxOpenFile(
                path,
                icon,
                icons,
                content,
                pinned,
                new JavaFxOpenFile.Lifecycle(
                        workingCopy,
                        registration,
                        closeGuard,
                        stateChanged,
                        extensions::showMessage,
                        this::filePinned,
                        this::removeFile));
    }

    private void reveal(JavaFxOpenFile file, EditorFileOpenRequest.Disposition disposition) {
        if (disposition == EditorFileOpenRequest.Disposition.PINNED) {
            if (!file.isPinned()) {
                file.pin();
            }
            area.select(file.tab());
        } else {
            area.preview(file.tab());
        }
    }

    private void discardPreview() {
        JavaFxOpenFile discarded = previewFile;
        previewFile = null;
        if (discarded != null && openFiles.remove(discarded.path(), discarded)) {
            area.removePreview(discarded.tab());
            discarded.close();
        }
    }

    private void filePinned(JavaFxOpenFile file) {
        if (previewFile == file) {
            previewFile = null;
            stateChanged.run();
        }
    }

    private void removeFile(JavaFxOpenFile file) {
        if (openFiles.remove(file.path(), file)) {
            if (previewFile == file) {
                previewFile = null;
            }
            file.close();
            stateChanged.run();
        }
    }

    private Optional<JavaFxOpenFile> selectedFile() {
        return openFiles.values().stream()
                .filter(file -> file.tab() == area.selectedTab())
                .findFirst();
    }

    private void showFileError(String heading, Exception exception) {
        extensions.showMessage(new EditorMessage(
                EditorMessageSeverity.ERROR,
                heading + ": " + Objects.requireNonNullElse(exception.getMessage(), exception.toString())));
    }

    @Override
    public void close() {
        requestRegistration.close();
        List<JavaFxOpenFile> files = List.copyOf(openFiles.values());
        files.forEach(file -> area.remove(file.tab()));
        files.forEach(JavaFxOpenFile::close);
        openFiles.clear();
        previewFile = null;
    }
}
