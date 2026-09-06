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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/** Synchronous declaration-ordered signal routing for one runtime. */
final class EndpointRouter {
    private final Map<RuntimeEndpointAddress, ActionBinding> actions = new LinkedHashMap<>();
    private final Map<RuntimeEndpointAddress, List<ActionBinding>> routes = new LinkedHashMap<>();
    private boolean active;

    /** Creates one signal emitter associated with a component in a composed world. */
    RuntimeSignal signal(RuntimeEndpointAddress address, EndpointDescriptor descriptor, BooleanSupplier enabled) {
        requireMatchingEndpoint(address, descriptor);
        return new RoutedSignal(address, descriptor.payload(), enabled);
    }

    /** Registers a payload-free action on a component in a composed world. */
    void action(
            RuntimeEndpointAddress address,
            EndpointDescriptor descriptor,
            BooleanSupplier enabled,
            RuntimeAction action) {
        requireMatchingEndpoint(address, descriptor);
        if (descriptor.payload().isPresent()) {
            throw new IllegalArgumentException("action requires a payload: " + descriptor.id());
        }
        register(address, descriptor, enabled, ignored -> action.execute());
    }

    /** Registers a payload-carrying action on a component in a composed world. */
    void action(
            RuntimeEndpointAddress address,
            EndpointDescriptor descriptor,
            BooleanSupplier enabled,
            RuntimePayloadAction action) {
        requireMatchingEndpoint(address, descriptor);
        if (descriptor.payload().isEmpty()) {
            throw new IllegalArgumentException("action does not accept a payload: " + descriptor.id());
        }
        register(address, descriptor, enabled, payload -> action.execute(payload.orElseThrow()));
    }

    /** Resolves declaration-ordered world connections after all component actions are registered. */
    void connect(List<RuntimeConnectionPlan> connections) {
        for (RuntimeConnectionPlan connection : connections) {
            connect(connection.signal(), connection.action(), connection.location() + "/action");
        }
    }

    /** Enables dispatch after composition has completed and runtime startup begins. */
    void activate() {
        active = true;
    }

    /** Stops dispatch and releases every action callback retained by the routing graph. */
    void deactivate() {
        active = false;
        actions.clear();
        routes.clear();
    }

    /** Adds one unique action implementation. */
    private void register(
            RuntimeEndpointAddress key, EndpointDescriptor descriptor, BooleanSupplier enabled, InternalAction action) {
        ActionBinding binding = new ActionBinding(key, descriptor.payload(), enabled, action);
        if (actions.putIfAbsent(key, binding) != null) {
            throw new IllegalArgumentException("action is already implemented: " + key);
        }
    }

    /** Adds one route after requiring its target action implementation. */
    private void connect(RuntimeEndpointAddress source, RuntimeEndpointAddress target, String location) {
        ActionBinding action = actions.get(target);
        if (action == null) {
            throw new RuntimeCompositionException(
                    RuntimeDiagnosticCode.COMPONENT_ENDPOINT_UNIMPLEMENTED,
                    "connected action has no runtime implementation: " + target,
                    location);
        }
        routes.computeIfAbsent(source, ignored -> new ArrayList<>()).add(action);
    }

    /** Requires absence or exact registered payload identity as declared. */
    private static void requirePayloadType(
            RuntimeEndpointAddress source, Optional<RegisteredType> expectedType, Optional<RuntimePayload> payload) {
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

    /** Requires a component endpoint address to agree with its safe descriptor. */
    private static void requireMatchingEndpoint(RuntimeEndpointAddress address, EndpointDescriptor descriptor) {
        if (!address.endpoint().value().equals(descriptor.id())) {
            throw new IllegalArgumentException(
                    "endpoint address and descriptor differ: " + address.endpoint() + " and " + descriptor.id());
        }
    }

    /** Uniform internal action accepting optional payload state. */
    @FunctionalInterface
    private interface InternalAction {
        void execute(Optional<RuntimePayload> payload);
    }

    /** Enabled-aware action binding. */
    private record ActionBinding(
            RuntimeEndpointAddress key,
            Optional<RegisteredType> expectedType,
            BooleanSupplier enabled,
            InternalAction action) {
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
        private final RuntimeEndpointAddress source;
        private final Optional<RegisteredType> payloadType;
        private final BooleanSupplier enabled;

        private RoutedSignal(
                RuntimeEndpointAddress source, Optional<RegisteredType> payloadType, BooleanSupplier enabled) {
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
