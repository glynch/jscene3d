/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.extension;

import io.github.glynch.jscene3d.editor.activity.ActivityId;
import io.github.glynch.jscene3d.editor.activity.EditorActivityContribution;
import io.github.glynch.jscene3d.editor.activity.EditorActivityRegistry;
import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.EditorCommand;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacementRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistration;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistry;
import io.github.glynch.jscene3d.editor.configuration.EditorConfiguration;
import io.github.glynch.jscene3d.editor.context.EditorContextCondition;
import io.github.glynch.jscene3d.editor.context.EditorContextKey;
import io.github.glynch.jscene3d.editor.context.EditorContextKeys;
import io.github.glynch.jscene3d.editor.diagnostic.DiagnosticCollectionId;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostic;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnosticCollection;
import io.github.glynch.jscene3d.editor.diagnostic.EditorDiagnostics;
import io.github.glynch.jscene3d.editor.extension.EditorExtension;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionContext;
import io.github.glynch.jscene3d.editor.extension.EditorExtensionDescriptor;
import io.github.glynch.jscene3d.editor.extension.EditorExtensions;
import io.github.glynch.jscene3d.editor.extension.project.EditorProjectContext;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.lifecycle.ExtensionSubscriptions;
import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import io.github.glynch.jscene3d.editor.menu.EditorMenuRegistry;
import io.github.glynch.jscene3d.editor.project.EditorProjects;
import io.github.glynch.jscene3d.editor.selection.EditorSelections;
import io.github.glynch.jscene3d.editor.status.EditorStatusBar;
import io.github.glynch.jscene3d.editor.status.EditorStatusItem;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemContribution;
import io.github.glynch.jscene3d.editor.status.EditorStatusItemState;
import io.github.glynch.jscene3d.editor.status.StatusItemId;
import io.github.glynch.jscene3d.editor.view.EditorViewContainers;
import io.github.glynch.jscene3d.editor.view.EditorViewContribution;
import io.github.glynch.jscene3d.editor.view.EditorViewRegistry;
import io.github.glynch.jscene3d.editor.view.ViewId;
import io.github.glynch.jscene3d.editor.window.EditorDialog;
import io.github.glynch.jscene3d.editor.window.EditorDialogButtonId;
import io.github.glynch.jscene3d.editor.window.EditorMessage;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import io.github.glynch.jscene3d.editor.workbench.command.EditorCommandMenuRegistry;
import io.github.glynch.jscene3d.editor.workbench.configuration.EditorConfigurationContext;
import io.github.glynch.jscene3d.editor.workbench.context.EditorContextState;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuSnapshot;
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
import java.util.function.Function;

/** Activates extensions and owns their toolkit-independent workbench contributions. */
public final class EditorExtensionHost implements AutoCloseable {
    private static final Consumer<EditorMessage> DEFAULT_MESSAGE_SINK =
            message -> System.getLogger(EditorExtensionHost.class.getName())
                    .log(System.Logger.Level.INFO, message.severity() + ": " + message.text());
    private static final Function<EditorDialog, Optional<EditorDialogButtonId>> DEFAULT_DIALOG_SINK =
            ignored -> Optional.empty();

    private final EditorProjectContext projects;
    private final EditorSelections selections;
    private final EditorConfiguration configuration;
    private final EditorContextState context = new EditorContextState();
    private final EditorRegistration projectContextRegistration;
    private final Map<String, ExtensionSubscriptionsImpl> activeExtensions = new LinkedHashMap<>();
    private final Map<String, EditorExtensionDescriptor> extensionDescriptors = new LinkedHashMap<>();
    private final List<Consumer<List<EditorExtensionDescriptor>>> extensionObservers = new ArrayList<>();
    private final Map<ActivityId, EditorActivityContribution> activities = new LinkedHashMap<>();
    private final List<Consumer<List<EditorActivityContribution>>> activityObservers = new ArrayList<>();
    private final Map<ViewId, EditorViewContribution> views = new LinkedHashMap<>();
    private final List<Consumer<List<EditorViewContribution>>> viewObservers = new ArrayList<>();
    private final List<Consumer<ViewId>> viewRequestObservers = new ArrayList<>();
    private final Map<StatusItemId, StatusItemRegistration> statusItems = new LinkedHashMap<>();
    private final List<Consumer<List<EditorStatusItemSnapshot>>> statusObservers = new ArrayList<>();
    private final Map<DiagnosticCollectionId, DiagnosticCollectionRegistration> diagnosticCollections =
            new LinkedHashMap<>();
    private final List<Consumer<List<EditorDiagnosticSnapshot>>> diagnosticObservers = new ArrayList<>();
    private final EditorWindow window = new WindowFacade();
    private final EditorCommandMenuRegistry commandMenus = new EditorCommandMenuRegistry(() -> window);
    private Consumer<EditorMessage> messageSink = DEFAULT_MESSAGE_SINK;
    private Function<EditorDialog, Optional<EditorDialogButtonId>> dialogSink = DEFAULT_DIALOG_SINK;
    private boolean closed;

    /**
     * Creates an empty host around the editor's current-project lifecycle.
     *
     * @param projects current-project lifecycle
     * @param selections shared editor selection
     */
    public EditorExtensionHost(EditorProjectContext projects, EditorSelections selections) {
        this(projects, selections, new EditorConfigurationContext());
    }

    /**
     * Creates an empty host with the window's live effective configuration.
     *
     * @param projects current-project lifecycle
     * @param selections shared editor selection
     * @param configuration effective project configuration
     */
    public EditorExtensionHost(
            EditorProjectContext projects, EditorSelections selections, EditorConfiguration configuration) {
        this.projects = Objects.requireNonNull(projects, "projects");
        this.selections = Objects.requireNonNull(selections, "selections");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        projectContextRegistration =
                this.projects.observe(project -> setContext(EditorContextKeys.PROJECT_OPEN, project.isPresent()));
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
        EditorExtensionDescriptor descriptor = Objects.requireNonNull(candidate.descriptor(), "extension.descriptor()");
        if (!descriptor.id().equals(id)) {
            throw new IllegalArgumentException(
                    "extension descriptor identity does not match extension identity: " + id);
        }
        ExtensionSubscriptionsImpl subscriptions = new ExtensionSubscriptionsImpl();
        EditorExtensionContext extensionContext = new Context(subscriptions);
        try {
            candidate.activate(extensionContext);
            activeExtensions.put(id, subscriptions);
            extensionDescriptors.put(id, descriptor);
            notifyExtensionObservers();
        } catch (RuntimeException failure) {
            subscriptions.close();
            throw failure;
        }
    }

    /**
     * Observes the complete ordered Activity Bar contribution snapshot.
     *
     * <p>The listener immediately receives the current snapshot.
     *
     * @param observer synchronous activity observer
     * @return removable listener registration
     */
    public EditorRegistration observeActivities(Consumer<List<EditorActivityContribution>> observer) {
        requireOpen();
        Consumer<List<EditorActivityContribution>> listener = Objects.requireNonNull(observer, "observer");
        activityObservers.add(listener);
        listener.accept(activitySnapshot());
        return once(() -> activityObservers.remove(listener));
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
     * Observes the complete ordered diagnostic snapshot published by every extension.
     *
     * <p>The listener immediately receives the current snapshot and is notified after every collection change.
     *
     * @param observer synchronous diagnostic observer
     * @return removable listener registration
     */
    public EditorRegistration observeDiagnostics(Consumer<List<EditorDiagnosticSnapshot>> observer) {
        requireOpen();
        Consumer<List<EditorDiagnosticSnapshot>> listener = Objects.requireNonNull(observer, "observer");
        diagnosticObservers.add(listener);
        listener.accept(diagnosticSnapshot());
        return once(() -> diagnosticObservers.remove(listener));
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

    /** Routes extension modal dialogs to the workbench's platform adapter. */
    public void showDialogsWith(Function<EditorDialog, Optional<EditorDialogButtonId>> sink) {
        requireOpen();
        dialogSink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Observes complete ordered menus with their currently placed command state.
     *
     * <p>The listener immediately receives the current snapshot.
     */
    public EditorRegistration observeMenus(Consumer<List<EditorMenuSnapshot>> observer) {
        requireOpen();
        return commandMenus.observe(observer);
    }

    /**
     * Invokes a registered command through the same path used by workbench actions.
     *
     * @param command registered command identity
     */
    public void execute(CommandId command) {
        executeCommand(command);
    }

    /**
     * Requests that the workbench reveal one registered view.
     *
     * @param view registered view identity
     */
    public void showView(ViewId view) {
        window.showView(view);
    }

    /** Shows one toolkit-independent application-modal dialog. */
    public Optional<EditorDialogButtonId> showDialog(EditorDialog dialog) {
        return window.showDialog(dialog);
    }

    /** Deactivates extensions in reverse order and removes every remaining contribution. */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        projectContextRegistration.close();
        List.copyOf(activeExtensions.values()).reversed().forEach(ExtensionSubscriptionsImpl::close);
        activeExtensions.clear();
        extensionDescriptors.clear();
        activities.clear();
        views.clear();
        commandMenus.close();
        statusItems.clear();
        diagnosticCollections.clear();
        notifyExtensionObservers();
        notifyActivityObservers();
        notifyViewObservers();
        notifyStatusObservers();
        notifyDiagnosticObservers();
        extensionObservers.clear();
        activityObservers.clear();
        viewObservers.clear();
        viewRequestObservers.clear();
        statusObservers.clear();
        diagnosticObservers.clear();
    }

    private EditorRegistration registerActivity(EditorActivityContribution contribution) {
        requireOpen();
        EditorActivityContribution registered = Objects.requireNonNull(contribution, "contribution");
        registered.views().forEach(this::requirePrimarySidebarView);
        Optional<EditorActivityContribution> owningActivity = activities.values().stream()
                .filter(activity -> activity.views().stream().anyMatch(registered.views()::contains))
                .findFirst();
        if (owningActivity.isPresent()) {
            throw new IllegalArgumentException("activity view already belongs to another container: "
                    + owningActivity.orElseThrow().id());
        }
        if (activities.putIfAbsent(registered.id(), registered) != null) {
            throw new IllegalArgumentException("activity identity is already registered: " + registered.id());
        }
        notifyActivityObservers();
        return once(() -> {
            if (activities.remove(registered.id(), registered)) {
                notifyActivityObservers();
            }
        });
    }

    private void requirePrimarySidebarView(ViewId view) {
        EditorViewContribution activityView = views.get(view);
        if (activityView == null) {
            throw new IllegalArgumentException("activity view is not registered: " + view);
        }
        if (!activityView.container().equals(EditorViewContainers.PRIMARY_SIDEBAR)) {
            throw new IllegalArgumentException("activity view must be contributed to the primary sidebar: " + view);
        }
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
        commandMenus.execute(command);
    }

    /** Registers one workbench or extension command. */
    public EditorCommandRegistration registerCommand(EditorCommandContribution contribution, EditorCommand command) {
        requireOpen();
        return commandMenus.registerCommand(contribution, command);
    }

    /** Registers one visual placement for a registered workbench or extension command. */
    public EditorRegistration registerCommandPlacement(EditorCommandPlacement placement) {
        requireOpen();
        return commandMenus.registerPlacement(placement);
    }

    /** Registers one toolkit-independent top-level menu. */
    public EditorRegistration registerMenu(EditorMenuContribution contribution) {
        requireOpen();
        return commandMenus.registerMenu(contribution);
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
                .filter(contribution -> matches(contribution.condition()))
                .sorted((left, right) -> Integer.compare(left.order(), right.order()))
                .toList();
    }

    private List<EditorActivityContribution> activitySnapshot() {
        return activities.values().stream()
                .filter(this::isAvailable)
                .sorted(Comparator.comparingInt(EditorActivityContribution::order))
                .toList();
    }

    /** Returns whether a view remains registered even when its context condition is currently false. */
    public boolean isViewRegistered(ViewId view) {
        requireOpen();
        return views.containsKey(Objects.requireNonNull(view, "view"));
    }

    private boolean isAvailable(EditorActivityContribution contribution) {
        return matches(contribution.condition())
                && contribution.views().stream()
                        .map(views::get)
                        .filter(Objects::nonNull)
                        .anyMatch(view -> matches(view.condition()));
    }

    private boolean matches(Optional<EditorContextCondition<?>> condition) {
        return condition.map(candidate -> candidate.matches(context)).orElse(true);
    }

    private <T> void setContext(EditorContextKey<T> key, T value) {
        if (context.set(key, value)) {
            notifyViewObservers();
            notifyActivityObservers();
        }
    }

    private void notifyActivityObservers() {
        List<EditorActivityContribution> snapshot = activitySnapshot();
        List.copyOf(activityObservers).forEach(observer -> observer.accept(snapshot));
    }

    private List<EditorExtensionDescriptor> extensionSnapshot() {
        return List.copyOf(extensionDescriptors.values());
    }

    private void notifyExtensionObservers() {
        List<EditorExtensionDescriptor> snapshot = extensionSnapshot();
        List.copyOf(extensionObservers).forEach(observer -> observer.accept(snapshot));
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

    private List<EditorDiagnosticSnapshot> diagnosticSnapshot() {
        return diagnosticCollections.values().stream()
                .flatMap(collection -> collection.diagnostics.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .map(diagnostic ->
                                        new EditorDiagnosticSnapshot(collection.id(), entry.getKey(), diagnostic))))
                .toList();
    }

    private void notifyDiagnosticObservers() {
        List<EditorDiagnosticSnapshot> snapshot = diagnosticSnapshot();
        List.copyOf(diagnosticObservers).forEach(observer -> observer.accept(snapshot));
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
        public EditorActivityRegistry activities() {
            return EditorExtensionHost.this::registerActivity;
        }

        @Override
        public EditorViewRegistry views() {
            return EditorExtensionHost.this::registerView;
        }

        @Override
        public EditorExtensions extensions() {
            return new EditorExtensions() {
                @Override
                public List<EditorExtensionDescriptor> installed() {
                    requireOpen();
                    return extensionSnapshot();
                }

                @Override
                public EditorRegistration observe(Consumer<List<EditorExtensionDescriptor>> observer) {
                    requireOpen();
                    Consumer<List<EditorExtensionDescriptor>> listener = Objects.requireNonNull(observer, "observer");
                    extensionObservers.add(listener);
                    listener.accept(extensionSnapshot());
                    return once(() -> extensionObservers.remove(listener));
                }
            };
        }

        @Override
        public EditorCommandRegistry commands() {
            return new EditorCommandRegistry() {
                @Override
                public EditorCommandRegistration register(
                        EditorCommandContribution contribution, EditorCommand command) {
                    return EditorExtensionHost.this.registerCommand(contribution, command);
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
        public EditorMenuRegistry menus() {
            return EditorExtensionHost.this::registerMenu;
        }

        @Override
        public EditorConfiguration configuration() {
            return configuration;
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
            if (!matches(views.get(id).condition())) {
                throw new IllegalStateException("view is not available in the current editor context: " + id);
            }
            List.copyOf(viewRequestObservers).forEach(observer -> observer.accept(id));
        }

        @Override
        public Optional<EditorDialogButtonId> showDialog(EditorDialog dialog) {
            requireOpen();
            return Objects.requireNonNull(dialogSink.apply(Objects.requireNonNull(dialog, "dialog")), "dialog result");
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
            diagnostics.put(
                    Objects.requireNonNull(source, "source"),
                    List.copyOf(Objects.requireNonNull(replacement, "replacement")));
            notifyDiagnosticObservers();
        }

        @Override
        public void replaceAll(Map<URI, List<EditorDiagnostic>> replacement) {
            requireCollectionOpen();
            Map<URI, List<EditorDiagnostic>> copied = new LinkedHashMap<>();
            Objects.requireNonNull(replacement, "replacement")
                    .forEach((source, items) -> copied.put(
                            Objects.requireNonNull(source, "source"),
                            List.copyOf(Objects.requireNonNull(items, "diagnostics"))));
            diagnostics.clear();
            diagnostics.putAll(copied);
            notifyDiagnosticObservers();
        }

        @Override
        public void clear(URI source) {
            requireCollectionOpen();
            if (diagnostics.remove(Objects.requireNonNull(source, "source")) != null) {
                notifyDiagnosticObservers();
            }
        }

        @Override
        public void clear() {
            requireCollectionOpen();
            if (!diagnostics.isEmpty()) {
                diagnostics.clear();
                notifyDiagnosticObservers();
            }
        }

        @Override
        public void close() {
            if (!collectionClosed) {
                collectionClosed = true;
                diagnostics.clear();
                if (diagnosticCollections.remove(id, this)) {
                    notifyDiagnosticObservers();
                }
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
