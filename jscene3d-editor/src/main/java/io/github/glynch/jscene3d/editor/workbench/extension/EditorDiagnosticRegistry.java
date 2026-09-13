/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostics;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Owns diagnostic collections and publishes their combined immutable snapshot. */
final class EditorDiagnosticRegistry implements EditorDiagnostics, AutoCloseable {
    private final Map<DiagnosticCollectionId, DiagnosticCollectionRegistration> collections = new LinkedHashMap<>();
    private final EditorSnapshotObservers<List<EditorDiagnosticSnapshot>> observers = new EditorSnapshotObservers<>();
    private boolean closed;

    @Override
    public EditorDiagnosticCollection createCollection(DiagnosticCollectionId id) {
        synchronized (this) {
            requireOpen();
            DiagnosticCollectionId identity = Objects.requireNonNull(id, "id");
            DiagnosticCollectionRegistration collection = new DiagnosticCollectionRegistration(identity);
            if (collections.putIfAbsent(identity, collection) != null) {
                throw new IllegalArgumentException("diagnostic collection identity is already registered: " + identity);
            }
            return collection;
        }
    }

    EditorRegistration observe(Consumer<List<EditorDiagnosticSnapshot>> observer) {
        synchronized (this) {
            requireOpen();
        }
        return observers.observe(observer, snapshot());
    }

    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            List.copyOf(collections.values()).forEach(DiagnosticCollectionRegistration::closeFromRegistry);
            collections.clear();
        }
        notifyObservers();
        observers.clear();
    }

    private synchronized List<EditorDiagnosticSnapshot> snapshot() {
        return collections.values().stream()
                .flatMap(collection -> collection.diagnostics.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .map(diagnostic ->
                                        new EditorDiagnosticSnapshot(collection.id(), entry.getKey(), diagnostic))))
                .toList();
    }

    private void notifyObservers() {
        observers.publish(snapshot());
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("diagnostic registry is closed");
        }
    }

    private final class DiagnosticCollectionRegistration implements EditorDiagnosticCollection {
        private final DiagnosticCollectionId id;
        private final Map<URI, List<EditorDiagnostic>> diagnostics = new LinkedHashMap<>();
        private boolean collectionClosed;

        private DiagnosticCollectionRegistration(DiagnosticCollectionId id) {
            this.id = id;
        }

        @Override
        public DiagnosticCollectionId id() {
            return id;
        }

        @Override
        public void replace(URI source, List<EditorDiagnostic> replacement) {
            synchronized (EditorDiagnosticRegistry.this) {
                requireCollectionOpen();
                diagnostics.put(
                        Objects.requireNonNull(source, "source"),
                        List.copyOf(Objects.requireNonNull(replacement, "replacement")));
            }
            notifyObservers();
        }

        @Override
        public void replaceAll(Map<URI, List<EditorDiagnostic>> replacement) {
            Map<URI, List<EditorDiagnostic>> copied = new LinkedHashMap<>();
            Objects.requireNonNull(replacement, "replacement")
                    .forEach((source, items) -> copied.put(
                            Objects.requireNonNull(source, "source"),
                            List.copyOf(Objects.requireNonNull(items, "diagnostics"))));
            synchronized (EditorDiagnosticRegistry.this) {
                requireCollectionOpen();
                diagnostics.clear();
                diagnostics.putAll(copied);
            }
            notifyObservers();
        }

        @Override
        public void clear(URI source) {
            boolean changed;
            synchronized (EditorDiagnosticRegistry.this) {
                requireCollectionOpen();
                changed = diagnostics.remove(Objects.requireNonNull(source, "source")) != null;
            }
            if (changed) {
                notifyObservers();
            }
        }

        @Override
        public void clear() {
            boolean changed;
            synchronized (EditorDiagnosticRegistry.this) {
                requireCollectionOpen();
                changed = !diagnostics.isEmpty();
                diagnostics.clear();
            }
            if (changed) {
                notifyObservers();
            }
        }

        @Override
        public void close() {
            boolean changed = false;
            synchronized (EditorDiagnosticRegistry.this) {
                if (!collectionClosed) {
                    collectionClosed = true;
                    diagnostics.clear();
                    changed = collections.remove(id, this);
                }
            }
            if (changed) {
                notifyObservers();
            }
        }

        private void closeFromRegistry() {
            collectionClosed = true;
            diagnostics.clear();
        }

        private void requireCollectionOpen() {
            if (collectionClosed) {
                throw new IllegalStateException("diagnostic collection is closed");
            }
        }
    }
}
