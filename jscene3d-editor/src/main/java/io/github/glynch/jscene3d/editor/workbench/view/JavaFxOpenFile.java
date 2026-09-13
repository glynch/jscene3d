/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.view;

import io.github.glynch.jscene3d.editor.builtin.text.EditorTextFileWorkingCopy;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorIcon;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorMessageSeverity;
import io.github.glynch.jscene3d.editor.workbench.dialog.EditorWindowCloseGuard;
import io.github.glynch.jscene3d.editor.workbench.icon.JavaFxIconRenderer;
import io.github.glynch.jscene3d.editor.workbench.style.EditorStyleClasses;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import org.jspecify.annotations.Nullable;

/** Owns one open workspace-file tab, its working copy, and close/save behavior. */
final class JavaFxOpenFile implements AutoCloseable {
    record Lifecycle(
            @Nullable EditorTextFileWorkingCopy workingCopy,
            EditorRegistration workingCopyRegistration,
            EditorWindowCloseGuard closeGuard,
            Runnable stateChanged,
            Consumer<EditorMessage> messages,
            Consumer<JavaFxOpenFile> removed) {}

    private final Path path;
    private final Tab tab = new Tab();
    private final JavaFxFileEditorContent content;
    private final @Nullable EditorTextFileWorkingCopy workingCopy;
    private final EditorRegistration workingCopyRegistration;
    private final EditorRegistration dirtyPresentationRegistration;
    private final Runnable stateChanged;
    private final Consumer<EditorMessage> messages;
    private boolean closed;

    JavaFxOpenFile(
            Path path,
            EditorIcon icon,
            JavaFxIconRenderer icons,
            JavaFxFileEditorContent content,
            Lifecycle lifecycle) {
        Lifecycle owner = Objects.requireNonNull(lifecycle, "lifecycle");
        this.path = Objects.requireNonNull(path, "path");
        this.content = Objects.requireNonNull(content, "content");
        this.workingCopy = owner.workingCopy();
        this.workingCopyRegistration =
                Objects.requireNonNull(owner.workingCopyRegistration(), "workingCopyRegistration");
        this.stateChanged = Objects.requireNonNull(owner.stateChanged(), "stateChanged");
        this.messages = Objects.requireNonNull(owner.messages(), "messages");

        Region dirty = createDirtyIndicator();
        Label title = new Label(path.getFileName().toString());
        title.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_TITLE);
        HBox graphic = new HBox(7.0, Objects.requireNonNull(icons, "icons").create(icon), title, dirty);
        graphic.setAlignment(Pos.CENTER_LEFT);
        graphic.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB);
        tab.setText(path.getFileName().toString());
        tab.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_GRAPHIC_ONLY);
        tab.setGraphic(graphic);
        tab.setContent(content.node());
        tab.setClosable(true);
        dirtyPresentationRegistration = observeDirtyPresentation(dirty);
        tab.setOnCloseRequest(event -> {
            if (!confirmClose(Objects.requireNonNull(owner.closeGuard(), "closeGuard"))) {
                event.consume();
            }
        });
        Consumer<JavaFxOpenFile> removed = Objects.requireNonNull(owner.removed(), "removed");
        tab.setOnClosed(ignored -> removed.accept(this));
    }

    Path path() {
        return path;
    }

    Tab tab() {
        return tab;
    }

    boolean isDirty() {
        return workingCopy != null && workingCopy.isDirty();
    }

    boolean save() {
        if (workingCopy == null) {
            return true;
        }
        try {
            workingCopy.save();
            stateChanged.run();
            return true;
        } catch (IOException exception) {
            showSaveError(exception);
            return false;
        }
    }

    void requestFocus() {
        content.focus().run();
    }

    void undo() {
        content.undo().run();
    }

    void redo() {
        content.redo().run();
    }

    private Region createDirtyIndicator() {
        Region dirty = new Region();
        dirty.getStyleClass().add(EditorStyleClasses.EDITOR_EDITOR_TAB_DIRTY);
        updateDirtyIndicator(dirty);
        return dirty;
    }

    private EditorRegistration observeDirtyPresentation(Region dirty) {
        return workingCopy == null
                ? () -> {}
                : workingCopy.onDidChangeDirty().subscribe(ignored -> updateDirtyIndicator(dirty));
    }

    private void updateDirtyIndicator(Region dirty) {
        boolean dirtyState = isDirty();
        dirty.setVisible(dirtyState);
        dirty.setManaged(dirtyState);
    }

    private boolean confirmClose(EditorWindowCloseGuard closeGuard) {
        return !isDirty() || closeGuard.confirmClose(path.getFileName().toString(), 1, this::save);
    }

    private void showSaveError(IOException exception) {
        messages.accept(new EditorMessage(
                EditorMessageSeverity.ERROR,
                "Unable to save " + path.getFileName() + ": "
                        + Objects.requireNonNullElse(exception.getMessage(), exception.toString())));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        dirtyPresentationRegistration.close();
        workingCopyRegistration.close();
        try {
            content.close().close();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to close file editor for " + path, exception);
        }
    }
}
