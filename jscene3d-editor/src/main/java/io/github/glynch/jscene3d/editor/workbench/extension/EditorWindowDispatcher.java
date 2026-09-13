/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Routes toolkit-independent window actions between extensions and workbench adapters. */
final class EditorWindowDispatcher implements EditorWindow, AutoCloseable {
    private static final Consumer<EditorMessage> DEFAULT_MESSAGE_SINK =
            message -> System.getLogger(EditorWindowDispatcher.class.getName())
                    .log(System.Logger.Level.INFO, message.severity() + ": " + message.text());
    private static final Function<EditorDialog, Optional<EditorDialogButtonId>> DEFAULT_DIALOG_SINK =
            ignored -> Optional.empty();

    private final EditorContributionRegistry contributions;
    private final List<Consumer<ViewId>> viewRequestObservers = new ArrayList<>();
    private final List<Consumer<EditorFileOpenRequest>> fileRequestObservers = new ArrayList<>();
    private Consumer<EditorMessage> messageSink = DEFAULT_MESSAGE_SINK;
    private Function<EditorDialog, Optional<EditorDialogButtonId>> dialogSink = DEFAULT_DIALOG_SINK;
    private boolean closed;

    EditorWindowDispatcher(EditorContributionRegistry contributions) {
        this.contributions = Objects.requireNonNull(contributions, "contributions");
    }

    EditorRegistration observeViewRequests(Consumer<ViewId> observer) {
        requireOpen();
        Consumer<ViewId> listener = Objects.requireNonNull(observer, "observer");
        viewRequestObservers.add(listener);
        return EditorRegistrationOnce.of(() -> viewRequestObservers.remove(listener));
    }

    EditorRegistration observeFileRequests(Consumer<EditorFileOpenRequest> observer) {
        requireOpen();
        Consumer<EditorFileOpenRequest> listener = Objects.requireNonNull(observer, "observer");
        fileRequestObservers.add(listener);
        return EditorRegistrationOnce.of(() -> fileRequestObservers.remove(listener));
    }

    void showMessagesWith(Consumer<EditorMessage> sink) {
        requireOpen();
        messageSink = Objects.requireNonNull(sink, "sink");
    }

    void showDialogsWith(Function<EditorDialog, Optional<EditorDialogButtonId>> sink) {
        requireOpen();
        dialogSink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void showMessage(EditorMessage message) {
        messageSink.accept(Objects.requireNonNull(message, "message"));
    }

    @Override
    public void showView(ViewId view) {
        requireOpen();
        ViewId id = Objects.requireNonNull(view, "view");
        contributions.requireViewAvailable(id);
        List.copyOf(viewRequestObservers).forEach(observer -> observer.accept(id));
    }

    @Override
    public void openFile(URI resource) {
        publishFileRequest(resource, EditorFileOpenRequest.Disposition.PINNED);
    }

    @Override
    public void previewFile(URI resource) {
        publishFileRequest(resource, EditorFileOpenRequest.Disposition.PREVIEW);
    }

    @Override
    public Optional<EditorDialogButtonId> showDialog(EditorDialog dialog) {
        requireOpen();
        return Objects.requireNonNull(dialogSink.apply(Objects.requireNonNull(dialog, "dialog")), "dialog result");
    }

    @Override
    public void close() {
        closed = true;
        viewRequestObservers.clear();
        fileRequestObservers.clear();
        messageSink = DEFAULT_MESSAGE_SINK;
        dialogSink = DEFAULT_DIALOG_SINK;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("window dispatcher is closed");
        }
    }

    private void publishFileRequest(URI resource, EditorFileOpenRequest.Disposition disposition) {
        requireOpen();
        EditorFileOpenRequest request =
                new EditorFileOpenRequest(Objects.requireNonNull(resource, "resource"), disposition);
        List.copyOf(fileRequestObservers).forEach(observer -> observer.accept(request));
    }
}
