/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.runtime.RuntimeAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeDiagnosticCode;
import io.github.glynch.jscene3d.project.runtime.RuntimePayloadAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Short-lived implementation context for one component's descriptor-declared endpoints. */
final class ComponentEndpointBindingContext implements ComponentEndpoints {
    private final ComponentEndpointBindingEntry binding;
    private final EndpointRouter router;
    private final Set<EndpointId> signals = new LinkedHashSet<>();
    private final Set<EndpointId> actions = new LinkedHashSet<>();
    private boolean active = true;

    /** Stores the component binding and world-owned router. */
    ComponentEndpointBindingContext(ComponentEndpointBindingEntry binding, EndpointRouter router) {
        this.binding = Objects.requireNonNull(binding, "binding");
        this.router = Objects.requireNonNull(router, "router");
    }

    @Override
    public RuntimeSignal signal(EndpointId endpoint) {
        EndpointDescriptor descriptor = requireEndpoint(binding.descriptor().signals(), endpoint, "signal");
        requireFirstBinding(signals, endpoint, "signal");
        return router.signal(address(endpoint), descriptor, binding.plan().owner()::isEnabled);
    }

    @Override
    public void action(EndpointId endpoint, RuntimeAction action) {
        EndpointDescriptor descriptor = requireEndpoint(binding.descriptor().actions(), endpoint, "action");
        requireFirstBinding(actions, endpoint, "action");
        router.action(
                address(endpoint),
                descriptor,
                binding.plan().owner()::isEnabled,
                Objects.requireNonNull(action, "action"));
    }

    @Override
    public void action(EndpointId endpoint, RuntimePayloadAction action) {
        EndpointDescriptor descriptor = requireEndpoint(binding.descriptor().actions(), endpoint, "action");
        requireFirstBinding(actions, endpoint, "action");
        router.action(
                address(endpoint),
                descriptor,
                binding.plan().owner()::isEnabled,
                Objects.requireNonNull(action, "action"));
    }

    /** Requires that the callback implemented every endpoint declared by safe metadata. */
    void requireComplete() {
        requireAllBound(binding.descriptor().signals(), signals, "signal");
        requireAllBound(binding.descriptor().actions(), actions, "action");
    }

    /** Prevents reuse of this composition-only context after its callback returns. */
    void expire() {
        active = false;
    }

    /** Returns one endpoint declared for the requested direction while enforcing context lifetime. */
    private EndpointDescriptor requireEndpoint(
            Map<EndpointId, EndpointDescriptor> declared, EndpointId endpoint, String kind) {
        requireActive();
        EndpointId validEndpoint = Objects.requireNonNull(endpoint, "endpoint");
        EndpointDescriptor descriptor = declared.get(validEndpoint);
        if (descriptor == null) {
            throw new IllegalArgumentException(kind + " is not declared: " + validEndpoint);
        }
        return descriptor;
    }

    /** Rejects duplicate binding of one endpoint within the callback. */
    private static void requireFirstBinding(Set<EndpointId> bound, EndpointId endpoint, String kind) {
        if (!bound.add(endpoint)) {
            throw new IllegalArgumentException(kind + " is already bound: " + endpoint);
        }
    }

    /** Reports the first descriptor-declared endpoint omitted by the runtime implementation. */
    private void requireAllBound(Map<EndpointId, EndpointDescriptor> declared, Set<EndpointId> bound, String kind) {
        for (EndpointId endpoint : declared.keySet()) {
            if (!bound.contains(endpoint)) {
                throw new RuntimeCompositionException(
                        RuntimeDiagnosticCode.COMPONENT_ENDPOINT_UNIMPLEMENTED,
                        "declared " + kind + " has no runtime implementation: " + endpoint,
                        binding.plan().location() + "/" + kind + "s/" + endpoint);
            }
        }
    }

    /** Returns the exact address of an endpoint on the component being bound. */
    private RuntimeEndpointAddress address(EndpointId endpoint) {
        return new RuntimeEndpointAddress(
                binding.plan().owner().id(), binding.plan().definition().id(), endpoint);
    }

    /** Requires use only from the active binding callback. */
    private void requireActive() {
        if (!active) {
            throw new IllegalStateException("component endpoint context has expired");
        }
    }
}
