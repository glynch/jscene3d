/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.status.EditorStatusBar;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Owns status-item state, deterministic ordering, observation, and disposal. */
final class EditorStatusItemRegistry implements EditorStatusBar, AutoCloseable {
    private final Map<StatusItemId, StatusItemRegistration> items = new LinkedHashMap<>();
    private final EditorSnapshotObservers<List<EditorStatusItemSnapshot>> observers = new EditorSnapshotObservers<>();
    private boolean closed;

    @Override
    public EditorStatusItem create(EditorStatusItemContribution contribution) {
        requireOpen();
        EditorStatusItemContribution metadata = Objects.requireNonNull(contribution, "contribution");
        StatusItemRegistration item = new StatusItemRegistration(metadata);
        if (items.putIfAbsent(metadata.id(), item) != null) {
            throw new IllegalArgumentException("status item identity is already registered: " + metadata.id());
        }
        notifyObservers();
        return item;
    }

    EditorRegistration observe(Consumer<List<EditorStatusItemSnapshot>> observer) {
        requireOpen();
        return observers.observe(observer, snapshot());
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List.copyOf(items.values()).forEach(StatusItemRegistration::closeFromRegistry);
        items.clear();
        notifyObservers();
        observers.clear();
    }

    private List<EditorStatusItemSnapshot> snapshot() {
        Comparator<EditorStatusItemSnapshot> order = Comparator.comparing(
                        (EditorStatusItemSnapshot item) -> item.contribution().alignment())
                .thenComparing(Comparator.comparingInt((EditorStatusItemSnapshot item) ->
                                item.contribution().priority())
                        .reversed())
                .thenComparing(item -> item.contribution().id().value());
        return items.values().stream()
                .map(StatusItemRegistration::snapshot)
                .sorted(order)
                .toList();
    }

    private void notifyObservers() {
        observers.publish(snapshot());
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("status item registry is closed");
        }
    }

    private final class StatusItemRegistration implements EditorStatusItem {
        private final EditorStatusItemContribution contribution;
        private EditorStatusItemState state =
                new EditorStatusItemState("Status", Optional.empty(), Optional.empty(), Optional.empty(), false);
        private boolean itemClosed;

        private StatusItemRegistration(EditorStatusItemContribution contribution) {
            this.contribution = contribution;
        }

        @Override
        public StatusItemId id() {
            return contribution.id();
        }

        @Override
        public void update(EditorStatusItemState updated) {
            requireItemOpen();
            state = Objects.requireNonNull(updated, "state");
            notifyObservers();
        }

        @Override
        public void close() {
            if (!itemClosed) {
                itemClosed = true;
                if (items.remove(id(), this)) {
                    notifyObservers();
                }
            }
        }

        private void closeFromRegistry() {
            itemClosed = true;
        }

        private void requireItemOpen() {
            if (itemClosed) {
                throw new IllegalStateException("status item is closed");
            }
        }

        private EditorStatusItemSnapshot snapshot() {
            return new EditorStatusItemSnapshot(contribution, state);
        }
    }
}
