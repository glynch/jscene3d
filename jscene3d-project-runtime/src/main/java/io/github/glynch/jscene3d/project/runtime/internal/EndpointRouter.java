/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.RegisteredType;
import io.github.glynch.jscene3d.project.runtime.RuntimeAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimePayload;
import io.github.glynch.jscene3d.project.runtime.RuntimePayloadAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.scene.SceneConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Synchronous declaration-ordered signal routing for one runtime. */
final class EndpointRouter {
    private final Map<EndpointKey, ActionBinding> actions = new LinkedHashMap<>();
    private final Map<EndpointKey, List<ActionBinding>> routes = new LinkedHashMap<>();
    private boolean active;

    /** Creates one signal emitter associated with a runtime node. */
    RuntimeSignal signal(String nodeId, EndpointDescriptor descriptor, BooleanSupplier enabled) {
        EndpointKey key = new EndpointKey(nodeId, descriptor.id());
        return new RoutedSignal(key, descriptor.payload(), enabled);
    }

    /** Registers an action declared without a payload. */
    void action(String nodeId, EndpointDescriptor descriptor, BooleanSupplier enabled, RuntimeAction action) {
        if (descriptor.payload().isPresent()) {
            throw new IllegalArgumentException("action requires a payload: " + descriptor.id());
        }
        register(nodeId, descriptor, enabled, ignored -> action.execute());
    }

    /** Registers an action declared with a payload. */
    void action(String nodeId, EndpointDescriptor descriptor, BooleanSupplier enabled, RuntimePayloadAction action) {
        if (descriptor.payload().isEmpty()) {
            throw new IllegalArgumentException("action does not accept a payload: " + descriptor.id());
        }
        register(nodeId, descriptor, enabled, payload -> action.execute(payload.orElseThrow()));
    }

    /** Resolves authored connections after all factories have registered their actions. */
    void connect(List<SceneConnection> connections) {
        for (int index = 0; index < connections.size(); index++) {
            SceneConnection connection = connections.get(index);
            EndpointKey source =
                    new EndpointKey(connection.from().node(), connection.from().signal());
            EndpointKey target =
                    new EndpointKey(connection.to().node(), connection.to().action());
            ActionBinding action = actions.get(target);
            if (action == null) {
                throw new RuntimeCompositionException(
                        RuntimeDiagnosticCode.ACTION_UNIMPLEMENTED,
                        "connected action has no runtime implementation: " + target,
                        "/connections/" + index + "/to");
            }
            routes.computeIfAbsent(source, ignored -> new ArrayList<>()).add(action);
        }
    }

    /** Enables dispatch after composition has completed and runtime startup begins. */
    void activate() {
        active = true;
    }

    /** Stops further signal dispatch during closure. */
    void deactivate() {
        active = false;
    }

    /** Adds one unique action implementation. */
    private void register(
            String nodeId, EndpointDescriptor descriptor, BooleanSupplier enabled, InternalAction action) {
        EndpointKey key = new EndpointKey(nodeId, descriptor.id());
        ActionBinding binding = new ActionBinding(key, descriptor.payload(), enabled, action);
        if (actions.putIfAbsent(key, binding) != null) {
            throw new IllegalArgumentException("action is already implemented: " + key);
        }
    }

    /** Requires absence or exact registered payload identity as declared. */
    private static void requirePayloadType(
            EndpointKey source, Optional<RegisteredType> expectedType, Optional<RuntimePayload> payload) {
        if (expectedType.isEmpty() && payload.isPresent()) {
            throw new IllegalArgumentException("signal does not accept a payload: " + source);
        }
        if (expectedType.isPresent() && payload.isEmpty()) {
            throw new IllegalArgumentException("signal requires payload " + expectedType.orElseThrow() + ": " + source);
        }
        if (expectedType.isPresent()
                && !expectedType.orElseThrow().equals(payload.orElseThrow().type())) {
            throw new IllegalArgumentException("signal requires payload " + expectedType.orElseThrow()
                    + " but received " + payload.orElseThrow().type() + ": " + source);
        }
    }

    /** Stable runtime endpoint key. */
    private record EndpointKey(String nodeId, String endpointId) {
        private EndpointKey {
            Preconditions.requireNonBlank(nodeId, "nodeId");
            Preconditions.requireNonBlank(endpointId, "endpointId");
        }
    }

    /** Uniform internal action accepting optional payload state. */
    @FunctionalInterface
    private interface InternalAction {
        void execute(Optional<RuntimePayload> payload);
    }

    /** Enabled-aware action binding. */
    private record ActionBinding(
            EndpointKey key, Optional<RegisteredType> expectedType, BooleanSupplier enabled, InternalAction action) {
        private ActionBinding {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(expectedType, "expectedType");
            Objects.requireNonNull(enabled, "enabled");
            Objects.requireNonNull(action, "action");
        }

        private void execute(Optional<RuntimePayload> payload) {
            requirePayloadType(key, expectedType, payload);
            if (enabled.getAsBoolean()) {
                action.execute(payload);
            }
        }
    }

    /** Runtime-facing emitter with descriptor-owned payload validation. */
    private final class RoutedSignal implements RuntimeSignal {
        private final EndpointKey source;
        private final Optional<RegisteredType> payloadType;
        private final BooleanSupplier enabled;

        private RoutedSignal(EndpointKey source, Optional<RegisteredType> payloadType, BooleanSupplier enabled) {
            this.source = source;
            this.payloadType = payloadType;
            this.enabled = enabled;
        }

        @Override
        public void emit() {
            emit(Optional.empty());
        }

        @Override
        public void emit(RuntimePayload payload) {
            emit(Optional.of(payload));
        }

        /** Dispatches against a snapshot of the current declaration-ordered routes. */
        private void emit(Optional<RuntimePayload> payload) {
            if (!active) {
                throw new IllegalStateException("runtime signal dispatch is not active");
            }
            requirePayloadType(source, payloadType, payload);
            if (!enabled.getAsBoolean()) {
                return;
            }
            for (ActionBinding action : List.copyOf(routes.getOrDefault(source, List.of()))) {
                action.execute(payload);
            }
        }
    }
}
