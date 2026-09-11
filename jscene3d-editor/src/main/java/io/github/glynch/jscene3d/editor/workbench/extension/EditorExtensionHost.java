/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommand;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacementRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistry;
import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostics;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.lifecycle.ExtensionSubscriptions;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.status.EditorStatusBar;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.EditorViewRegistry;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import io.github.glynch.jscene3d.editor.workbench.status.EditorStatusItemSnapshot;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Activates extensions and owns their toolkit-independent workbench contributions. */
public final class EditorExtensionHost implements AutoCloseable {
    private static final Consumer<EditorMessage> DEFAULT_MESSAGE_SINK =
            message -> System.getLogger(EditorExtensionHost.class.getName())
                    .log(System.Logger.Level.INFO, message.severity() + ": " + message.text());

    private final EditorProjectContext projects;
    private final EditorSelections selections;
    private final Map<String, ExtensionSubscriptionsImpl> activeExtensions = new LinkedHashMap<>();
    private final Map<ViewId, EditorViewContribution> views = new LinkedHashMap<>();
    private final List<Consumer<List<EditorViewContribution>>> viewObservers = new ArrayList<>();
    private final List<Consumer<ViewId>> viewRequestObservers = new ArrayList<>();
    private final Map<CommandId, EditorCommand> commands = new LinkedHashMap<>();
    private final List<EditorCommandPlacement> commandPlacements = new ArrayList<>();
    private final Map<StatusItemId, StatusItemRegistration> statusItems = new LinkedHashMap<>();
    private final List<Consumer<List<EditorStatusItemSnapshot>>> statusObservers = new ArrayList<>();
    private final Map<DiagnosticCollectionId, DiagnosticCollectionRegistration> diagnosticCollections =
            new LinkedHashMap<>();
    private final EditorWindow window = new WindowFacade();
    private Consumer<EditorMessage> messageSink = DEFAULT_MESSAGE_SINK;
    private boolean closed;

    /**
     * Creates an empty host around the editor's current-project lifecycle.
     *
     * @param projects current-project lifecycle
     * @param selections shared editor selection
     */
    public EditorExtensionHost(EditorProjectContext projects, EditorSelections selections) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.selections = Objects.requireNonNull(selections, "selections");
    }

    /**
     * Activates one extension exactly once for this host's lifetime.
     *
     * @param extension extension to activate
     * @throws IllegalArgumentException if its identity is blank or already active
     * @throws IllegalStateException if the host has closed
     */
    public void activate(EditorExtension extension) {
        requireOpen();
        EditorExtension candidate = Objects.requireNonNull(extension, "extension");
        String id = Objects.requireNonNull(candidate.id(), "extension.id()");
        if (id.isBlank()) {
            throw new IllegalArgumentException("extension identity must not be blank");
        }
        if (activeExtensions.containsKey(id)) {
            throw new IllegalArgumentException("extension identity is already active: " + id);
        }
        ExtensionSubscriptionsImpl subscriptions = new ExtensionSubscriptionsImpl();
        EditorExtensionContext context = new Context(subscriptions);
        try {
            candidate.activate(context);
            activeExtensions.put(id, subscriptions);
        } catch (RuntimeException failure) {
            subscriptions.close();
            throw failure;
        }
    }

    /**
     * Observes the complete ordered view-contribution snapshot.
     *
     * <p>The listener immediately receives the current snapshot.
     *
     * @param observer synchronous view observer
     * @return removable listener registration
     */
    public EditorRegistration observeViews(Consumer<List<EditorViewContribution>> observer) {
        requireOpen();
        Consumer<List<EditorViewContribution>> listener = Objects.requireNonNull(observer, "observer");
        viewObservers.add(listener);
        listener.accept(viewSnapshot());
        return once(() -> viewObservers.remove(listener));
    }

    /**
     * Observes requests to reveal a registered view.
     *
     * @param observer synchronous view-request observer
     * @return removable listener registration
     */
    public EditorRegistration observeViewRequests(Consumer<ViewId> observer) {
        requireOpen();
        Consumer<ViewId> listener = Objects.requireNonNull(observer, "observer");
        viewRequestObservers.add(listener);
        return once(() -> viewRequestObservers.remove(listener));
    }

    /**
     * Observes the complete ordered status-item snapshot.
     *
     * <p>The listener immediately receives the current snapshot and is notified after every item
     * creation, update, or removal.
     *
     * @param observer synchronous status-item observer
     * @return removable listener registration
     */
    public EditorRegistration observeStatusItems(Consumer<List<EditorStatusItemSnapshot>> observer) {
        requireOpen();
        Consumer<List<EditorStatusItemSnapshot>> listener = Objects.requireNonNull(observer, "observer");
        statusObservers.add(listener);
        listener.accept(statusSnapshot());
        return once(() -> statusObservers.remove(listener));
    }

    /**
     * Routes extension window messages to the workbench presentation.
     *
     * @param sink workbench-owned message sink
     */
    public void showMessagesWith(Consumer<EditorMessage> sink) {
        requireOpen();
        messageSink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Invokes a registered command through the same path used by workbench actions.
     *
     * @param command registered command identity
     */
    public void execute(CommandId command) {
        executeCommand(command);
    }

    /** Deactivates extensions in reverse order and removes every remaining contribution. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        List.copyOf(activeExtensions.values()).reversed().forEach(ExtensionSubscriptionsImpl::close);
        activeExtensions.clear();
        views.clear();
        commands.clear();
        commandPlacements.clear();
        statusItems.clear();
        diagnosticCollections.clear();
        notifyViewObservers();
        notifyStatusObservers();
        viewObservers.clear();
        viewRequestObservers.clear();
        statusObservers.clear();
    }

    private EditorRegistration registerView(EditorViewContribution contribution) {
        requireOpen();
        EditorViewContribution registered = Objects.requireNonNull(contribution, "contribution");
        ViewId id = registered.view().id();
        if (views.putIfAbsent(id, registered) != null) {
            throw new IllegalArgumentException("view identity is already registered: " + id);
        }
        notifyViewObservers();
        return once(() -> {
            if (views.remove(id, registered)) {
                notifyViewObservers();
            }
        });
    }

    private void executeCommand(CommandId command) {
        requireOpen();
        CommandId id = Objects.requireNonNull(command, "command");
        EditorCommand registered = commands.get(id);
        if (registered == null) {
            throw new IllegalArgumentException("command identity is not registered: " + id);
        }
        registered.execute(() -> window);
    }

    private EditorRegistration registerCommandPlacement(EditorCommandPlacement placement) {
        requireOpen();
        EditorCommandPlacement registered = Objects.requireNonNull(placement, "placement");
        commandPlacements.add(registered);
        return once(() -> commandPlacements.remove(registered));
    }

    private EditorStatusItem createStatusItem(EditorStatusItemContribution contribution) {
        requireOpen();
        EditorStatusItemContribution metadata = Objects.requireNonNull(contribution, "contribution");
        StatusItemRegistration item = new StatusItemRegistration(metadata);
        if (statusItems.putIfAbsent(metadata.id(), item) != null) {
            throw new IllegalArgumentException("status item identity is already registered: " + metadata.id());
        }
        notifyStatusObservers();
        return item;
    }

    private EditorDiagnosticCollection createDiagnosticCollection(DiagnosticCollectionId id) {
        requireOpen();
        DiagnosticCollectionId identity = Objects.requireNonNull(id, "id");
        DiagnosticCollectionRegistration collection = new DiagnosticCollectionRegistration(identity);
        if (diagnosticCollections.putIfAbsent(identity, collection) != null) {
            throw new IllegalArgumentException("diagnostic collection identity is already registered: " + identity);
        }
        return collection;
    }

    private List<EditorViewContribution> viewSnapshot() {
        return views.values().stream()
                .sorted((left, right) -> Integer.compare(left.order(), right.order()))
                .toList();
    }

    private void notifyViewObservers() {
        List<EditorViewContribution> snapshot = viewSnapshot();
        List.copyOf(viewObservers).forEach(observer -> observer.accept(snapshot));
    }

    private List<EditorStatusItemSnapshot> statusSnapshot() {
        Comparator<EditorStatusItemSnapshot> order = Comparator.comparing(
                        (EditorStatusItemSnapshot item) -> item.contribution().alignment())
                .thenComparing(Comparator.comparingInt((EditorStatusItemSnapshot item) ->
                                item.contribution().priority())
                        .reversed())
                .thenComparing(item -> item.contribution().id().value());
        return statusItems.values().stream()
                .map(StatusItemRegistration::snapshot)
                .sorted(order)
                .toList();
    }

    private void notifyStatusObservers() {
        List<EditorStatusItemSnapshot> snapshot = statusSnapshot();
        List.copyOf(statusObservers).forEach(observer -> observer.accept(snapshot));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("extension host is closed");
        }
    }

    private static EditorRegistration once(Runnable removal) {
        AtomicBoolean registrationClosed = new AtomicBoolean();
        return () -> {
            if (registrationClosed.compareAndSet(false, true)) {
                removal.run();
            }
        };
    }

    private final class Context implements EditorExtensionContext {
        private final ExtensionSubscriptions subscriptions;

        private Context(ExtensionSubscriptions subscriptions) {
            this.subscriptions = subscriptions;
        }

        @Override
        public EditorViewRegistry views() {
            return EditorExtensionHost.this::registerView;
        }

        @Override
        public EditorCommandRegistry commands() {
            return new EditorCommandRegistry() {
                @Override
                public EditorRegistration register(EditorCommandContribution contribution, EditorCommand command) {
                    return registerCommand(contribution, command);
                }

                @Override
                public void execute(CommandId command) {
                    executeCommand(command);
                }
            };
        }

        @Override
        public EditorCommandPlacementRegistry commandPlacements() {
            return EditorExtensionHost.this::registerCommandPlacement;
        }

        @Override
        public EditorStatusBar statusBar() {
            return EditorExtensionHost.this::createStatusItem;
        }

        @Override
        public EditorWindow window() {
            return window;
        }

        @Override
        public EditorDiagnostics diagnostics() {
            return EditorExtensionHost.this::createDiagnosticCollection;
        }

        @Override
        public EditorProjects projects() {
            return projects;
        }

        @Override
        public EditorSelections selections() {
            return selections;
        }

        @Override
        public ExtensionSubscriptions subscriptions() {
            return subscriptions;
        }

        private EditorRegistration registerCommand(EditorCommandContribution contribution, EditorCommand command) {
            requireOpen();
            EditorCommandContribution metadata = Objects.requireNonNull(contribution, "contribution");
            EditorCommand registered = Objects.requireNonNull(command, "command");
            if (commands.putIfAbsent(metadata.id(), registered) != null) {
                throw new IllegalArgumentException("command identity is already registered: " + metadata.id());
            }
            return once(() -> commands.remove(metadata.id(), registered));
        }
    }

    private final class WindowFacade implements EditorWindow {
        @Override
        public void showMessage(EditorMessage message) {
            messageSink.accept(Objects.requireNonNull(message, "message"));
        }

        @Override
        public void showView(ViewId view) {
            requireOpen();
            ViewId id = Objects.requireNonNull(view, "view");
            if (!views.containsKey(id)) {
                throw new IllegalArgumentException("view identity is not registered: " + id);
            }
            List.copyOf(viewRequestObservers).forEach(observer -> observer.accept(id));
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
            notifyStatusObservers();
        }

        @Override
        public void close() {
            if (!itemClosed) {
                itemClosed = true;
                if (statusItems.remove(id(), this)) {
                    notifyStatusObservers();
                }
            }
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
            requireCollectionOpen();
            diagnostics.put(Objects.requireNonNull(source, "source"), List.copyOf(replacement));
        }

        @Override
        public void clear(URI source) {
            requireCollectionOpen();
            diagnostics.remove(Objects.requireNonNull(source, "source"));
        }

        @Override
        public void clear() {
            requireCollectionOpen();
            diagnostics.clear();
        }

        @Override
        public void close() {
            if (!collectionClosed) {
                collectionClosed = true;
                diagnostics.clear();
                diagnosticCollections.remove(id, this);
            }
        }

        private void requireCollectionOpen() {
            if (collectionClosed) {
                throw new IllegalStateException("diagnostic collection is closed");
            }
        }
    }

    private static final class ExtensionSubscriptionsImpl implements ExtensionSubscriptions {
        private final List<EditorRegistration> registrations = new ArrayList<>();
        private boolean subscriptionsClosed;

        @Override
        public <T extends EditorRegistration> T add(T registration) {
            if (subscriptionsClosed) {
                throw new IllegalStateException("extension subscriptions are closed");
            }
            T owned = Objects.requireNonNull(registration, "registration");
            registrations.add(owned);
            return owned;
        }

        private void close() {
            if (subscriptionsClosed) {
                return;
            }
            subscriptionsClosed = true;
            List.copyOf(registrations).reversed().forEach(EditorRegistration::close);
            registrations.clear();
        }
    }
}
