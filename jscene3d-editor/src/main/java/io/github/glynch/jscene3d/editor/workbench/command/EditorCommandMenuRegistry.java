/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.command;

import io.github.glynch.jscene3d.editor.command.CommandId;
import io.github.glynch.jscene3d.editor.command.CommandLocationId;
import io.github.glynch.jscene3d.editor.command.EditorCommand;
import io.github.glynch.jscene3d.editor.command.EditorCommandContext;
import io.github.glynch.jscene3d.editor.command.EditorCommandContribution;
import io.github.glynch.jscene3d.editor.command.EditorCommandKind;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacement;
import io.github.glynch.jscene3d.editor.command.EditorCommandPlacementRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistration;
import io.github.glynch.jscene3d.editor.command.EditorCommandRegistry;
import io.github.glynch.jscene3d.editor.command.EditorCommandState;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.menu.EditorMenuContribution;
import io.github.glynch.jscene3d.editor.menu.EditorMenuRegistry;
import io.github.glynch.jscene3d.editor.window.EditorWindow;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuCommandSnapshot;
import io.github.glynch.jscene3d.editor.workbench.menu.EditorMenuSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Owns command behavior, state, placements, and complete menu snapshots. */
public final class EditorCommandMenuRegistry
        implements EditorCommandRegistry, EditorCommandPlacementRegistry, EditorMenuRegistry, AutoCloseable {
    private final EditorCommandContext context;
    private final Map<CommandId, CommandRegistration> commands = new LinkedHashMap<>();
    private final List<EditorCommandPlacement> placements = new ArrayList<>();
    private final Map<CommandLocationId, EditorMenuContribution> menus = new LinkedHashMap<>();
    private final List<Consumer<List<EditorMenuSnapshot>>> observers = new ArrayList<>();
    private boolean closed;

    /** Creates an empty registry with the context supplied to command executions. */
    public EditorCommandMenuRegistry(EditorCommandContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    /** Registers one command and returns its state and lifecycle handle. */
    public EditorCommandRegistration registerCommand(EditorCommandContribution contribution, EditorCommand command) {
        requireOpen();
        EditorCommandContribution metadata = Objects.requireNonNull(contribution, "contribution");
        CommandRegistration registration =
                new CommandRegistration(metadata, Objects.requireNonNull(command, "command"));
        if (commands.putIfAbsent(metadata.id(), registration) != null) {
            throw new IllegalArgumentException("command identity is already registered: " + metadata.id());
        }
        notifyObservers();
        return registration;
    }

    /** Registers one command through the extension-facing command registry. */
    @Override
    public EditorCommandRegistration register(EditorCommandContribution contribution, EditorCommand command) {
        return registerCommand(contribution, command);
    }

    /** Registers one placement for an existing command. */
    public EditorRegistration registerPlacement(EditorCommandPlacement placement) {
        requireOpen();
        EditorCommandPlacement registered = Objects.requireNonNull(placement, "placement");
        if (!commands.containsKey(registered.command())) {
            throw new IllegalArgumentException("command identity is not registered: " + registered.command());
        }
        if (containsPlacement(registered)) {
            throw new IllegalArgumentException(
                    "command is already placed at location: " + registered.command() + " -> " + registered.location());
        }
        placements.add(registered);
        notifyObservers();
        return once(() -> {
            if (placements.remove(registered)) {
                notifyObservers();
            }
        });
    }

    /** Registers one placement through the extension-facing placement registry. */
    @Override
    public EditorRegistration register(EditorCommandPlacement placement) {
        return registerPlacement(placement);
    }

    /** Registers one top-level menu declaration. */
    public EditorRegistration registerMenu(EditorMenuContribution contribution) {
        requireOpen();
        EditorMenuContribution registered = Objects.requireNonNull(contribution, "contribution");
        if (menus.putIfAbsent(registered.location(), registered) != null) {
            throw new IllegalArgumentException("menu location is already registered: " + registered.location());
        }
        notifyObservers();
        return once(() -> {
            if (menus.remove(registered.location(), registered)) {
                notifyObservers();
            }
        });
    }

    /** Registers one menu through the extension-facing menu registry. */
    @Override
    public EditorRegistration register(EditorMenuContribution contribution) {
        return registerMenu(contribution);
    }

    /** Executes an enabled registered command. */
    public void execute(CommandId command) {
        execute(command, context);
    }

    /** Executes an enabled registered command with one semantic invocation argument. */
    @Override
    public void execute(CommandId command, Object argument) {
        execute(command, new ContextualCommandContext(context, argument));
    }

    private void execute(CommandId command, EditorCommandContext invocation) {
        requireOpen();
        CommandId id = Objects.requireNonNull(command, "command");
        CommandRegistration registered = commands.get(id);
        if (registered == null) {
            throw new IllegalArgumentException("command identity is not registered: " + id);
        }
        registered.execute(invocation);
    }

    /** Observes complete ordered menu snapshots and immediately receives the current state. */
    public EditorRegistration observe(Consumer<List<EditorMenuSnapshot>> observer) {
        requireOpen();
        Consumer<List<EditorMenuSnapshot>> listener = Objects.requireNonNull(observer, "observer");
        observers.add(listener);
        listener.accept(snapshot());
        return once(() -> observers.remove(listener));
    }

    /** Returns the current ordered commands placed for one semantic item context. */
    public List<EditorMenuCommandSnapshot> commandsAt(CommandLocationId location, Optional<String> contextValue) {
        requireOpen();
        return menuCommands(
                Objects.requireNonNull(location, "location"), Objects.requireNonNull(contextValue, "contextValue"));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        commands.clear();
        placements.clear();
        menus.clear();
        notifyObservers();
        observers.clear();
    }

    private boolean containsPlacement(EditorCommandPlacement placement) {
        return placements.stream()
                .anyMatch(candidate -> candidate.command().equals(placement.command())
                        && candidate.location().equals(placement.location()));
    }

    private List<EditorMenuSnapshot> snapshot() {
        return menus.values().stream()
                .sorted(Comparator.comparingInt(EditorMenuContribution::order)
                        .thenComparing(menu -> menu.location().value()))
                .map(menu -> new EditorMenuSnapshot(menu, menuCommands(menu.location(), Optional.empty())))
                .toList();
    }

    private List<EditorMenuCommandSnapshot> menuCommands(
            CommandLocationId location, Optional<String> invocationContext) {
        return placements.stream()
                .filter(placement -> placement.location().equals(location))
                .filter(placement -> matchesContext(placement.contextValue(), invocationContext))
                .sorted(Comparator.comparingInt(EditorCommandPlacement::order)
                        .thenComparing(placement -> placement.command().value()))
                .flatMap(placement -> Optional.ofNullable(commands.get(placement.command()))
                        .map(command -> new EditorMenuCommandSnapshot(
                                command.contribution, command.state, placement.group(), placement.order()))
                        .stream())
                .toList();
    }

    private static boolean matchesContext(Optional<String> placementContext, Optional<String> invocationContext) {
        return placementContext.isEmpty() || placementContext.equals(invocationContext);
    }

    private void notifyObservers() {
        List<EditorMenuSnapshot> current = snapshot();
        List.copyOf(observers).forEach(observer -> observer.accept(current));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("command and menu registry is closed");
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

    private final class CommandRegistration implements EditorCommandRegistration {
        private final EditorCommandContribution contribution;
        private final EditorCommand command;
        private EditorCommandState state = EditorCommandState.ENABLED_STATE;
        private boolean registrationClosed;

        private CommandRegistration(EditorCommandContribution contribution, EditorCommand command) {
            this.contribution = contribution;
            this.command = command;
        }

        @Override
        public void update(EditorCommandState replacement) {
            requireOpen();
            requireRegistrationOpen();
            EditorCommandState updated = Objects.requireNonNull(replacement, "state");
            if (contribution.kind() == EditorCommandKind.ACTION && updated.selected()) {
                throw new IllegalArgumentException("action command cannot be selected: " + contribution.id());
            }
            if (!state.equals(updated)) {
                state = updated;
                notifyObservers();
            }
        }

        @Override
        public void close() {
            if (!registrationClosed) {
                registrationClosed = true;
                if (commands.remove(contribution.id(), this)) {
                    notifyObservers();
                }
            }
        }

        private void execute(EditorCommandContext invocation) {
            requireRegistrationOpen();
            if (!state.enabled()) {
                throw new IllegalStateException("command is disabled: " + contribution.id());
            }
            command.execute(invocation);
        }

        private void requireRegistrationOpen() {
            if (registrationClosed) {
                throw new IllegalStateException("command registration is closed: " + contribution.id());
            }
        }
    }

    private record ContextualCommandContext(EditorCommandContext delegate, Object invocationArgument)
            implements EditorCommandContext {
        private ContextualCommandContext {
            Objects.requireNonNull(delegate, "delegate");
            Objects.requireNonNull(invocationArgument, "argument");
        }

        @Override
        public EditorWindow window() {
            return delegate.window();
        }

        @Override
        public Optional<Object> argument() {
            return Optional.of(invocationArgument);
        }
    }
}
