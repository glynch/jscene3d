/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor;

import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.extension.EndpointDescriptor;
import io.github.glynch.jscene3d.project.extension.ExtensionDescriptor;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpoints;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentFactoryRegistry;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentLifecycleCallbacks;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceBinder;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentReferenceResolver;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentRuntimeExtension;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentUpdateCallbacks;
import java.util.Map;
import java.util.Objects;

/** Supplies descriptor-complete inert component values for non-presentation editor preview content. */
final class InertComponentRuntimeExtension implements ComponentRuntimeExtension {
    private final ExtensionDescriptor descriptor;

    /** Retains the safe metadata whose executable application implementation must remain unloaded. */
    InertComponentRuntimeExtension(ExtensionDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
    }

    @Override
    public String id() {
        return descriptor.id();
    }

    @Override
    public void register(ComponentFactoryRegistry registry) {
        ComponentFactoryRegistry validRegistry = Objects.requireNonNull(registry, "registry");
        for (ComponentTypeDescriptor component : descriptor.components()) {
            validRegistry.register(component.type(), context -> new InertComponent(context.descriptor()));
        }
    }

    /** Satisfies descriptor-declared runtime protocols while deliberately performing no game operation. */
    private static final class InertComponent
            implements ComponentLifecycleCallbacks,
                    ComponentUpdateCallbacks,
                    ComponentReferenceBinder,
                    ComponentEndpointBinder {
        private final ComponentTypeDescriptor descriptor;

        /** Retains the descriptor required to bind every inert endpoint. */
        private InertComponent(ComponentTypeDescriptor descriptor) {
            this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        }

        @Override
        public void bindReferences(ComponentReferenceResolver references) {
            Objects.requireNonNull(references, "references");
            // Preview components deliberately do not dereference gameplay targets.
        }

        @Override
        public void bindEndpoints(ComponentEndpoints endpoints) {
            ComponentEndpoints validEndpoints = Objects.requireNonNull(endpoints, "endpoints");
            for (EndpointId signal : descriptor.signals().keySet()) {
                validEndpoints.signal(signal);
            }
            for (Map.Entry<EndpointId, EndpointDescriptor> action :
                    descriptor.actions().entrySet()) {
                bindAction(validEndpoints, action.getKey(), action.getValue());
            }
        }

        /** Registers the correct no-op callback shape for one declared action. */
        private static void bindAction(ComponentEndpoints endpoints, EndpointId id, EndpointDescriptor descriptor) {
            if (descriptor.payload().isPresent()) {
                endpoints.action(id, payload -> {});
            } else {
                endpoints.action(id, () -> {});
            }
        }
    }
}
