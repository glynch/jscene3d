/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentTypeDescriptor;
import io.github.glynch.jscene3d.project.runtime.extension.ComponentEndpointBinder;
import java.util.Objects;

/** Deferred endpoint-binding callback with its descriptor-authoritative context. */
record ComponentEndpointBindingEntry(
        ComponentPlan plan, ComponentTypeDescriptor descriptor, ComponentEndpointBinder binder) {
    /** Validates the deferred endpoint binding. */
    ComponentEndpointBindingEntry {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(binder, "binder");
    }
}
