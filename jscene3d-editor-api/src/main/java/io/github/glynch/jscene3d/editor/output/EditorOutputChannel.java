/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.output;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.view.EditorView;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.view.ViewKindId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Toolkit-independent, read-only text output contributed as an editor view. */
public final class EditorOutputChannel implements EditorView, EditorRegistration {
    /** Stable presentation kind used by workbench output adapters. */
    public static final ViewKindId VIEW_KIND = new ViewKindId("io.github.glynch.jscene3d.editor.view.output-channel");

    private final ViewId id;
    private final String title;
    private final List<Consumer<String>> observers = new ArrayList<>();

    private String content = "";
    private boolean closed;

    /**
     * Creates an empty output channel with a stable view identity.
     *
     * @param id stable view identity
     * @param title human-readable channel title
     */
    public EditorOutputChannel(ViewId id, String title) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = requireTitle(title);
    }

    @Override
    public ViewId id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public ViewKindId kind() {
        return VIEW_KIND;
    }

    /**
     * Returns the channel's complete current content.
     *
     * @return complete current content
     */
    public synchronized String content() {
        return content;
    }

    /**
     * Replaces the complete output and publishes one atomic update.
     *
     * @param replacement complete replacement content
     */
    public void replace(String replacement) {
        List<Consumer<String>> listeners;
        String snapshot = Objects.requireNonNull(replacement, "replacement");
        synchronized (this) {
            requireOpen();
            content = snapshot;
            listeners = List.copyOf(observers);
        }
        listeners.forEach(observer -> observer.accept(snapshot));
    }

    /**
     * Appends output and publishes the resulting complete content.
     *
     * @param addition content to append
     */
    public void append(String addition) {
        List<Consumer<String>> listeners;
        String snapshot;
        synchronized (this) {
            requireOpen();
            content += Objects.requireNonNull(addition, "addition");
            snapshot = content;
            listeners = List.copyOf(observers);
        }
        String published = snapshot;
        listeners.forEach(observer -> observer.accept(published));
    }

    /** Clears every line currently retained by the channel. */
    public void clear() {
        replace("");
    }

    /**
     * Observes complete output snapshots and immediately receives the current content.
     *
     * @param observer synchronous output-snapshot observer
     * @return removable observer registration
     */
    public EditorRegistration observe(Consumer<String> observer) {
        Consumer<String> listener = Objects.requireNonNull(observer, "observer");
        String snapshot;
        synchronized (this) {
            requireOpen();
            observers.add(listener);
            snapshot = content;
        }
        listener.accept(snapshot);
        return () -> removeObserver(listener);
    }

    /** Closes the channel and removes every observer. */
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        observers.clear();
    }

    private synchronized void removeObserver(Consumer<String> observer) {
        observers.remove(observer);
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("output channel is closed");
        }
    }

    private static String requireTitle(String candidate) {
        String value = Objects.requireNonNull(candidate, "title");
        if (value.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        return value;
    }
}
