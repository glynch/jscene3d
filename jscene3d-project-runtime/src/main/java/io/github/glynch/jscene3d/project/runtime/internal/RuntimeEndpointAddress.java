/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.runtime.RuntimeEntityId;
import java.util.Objects;

/** Stable address of one component endpoint in a particular live world. */
record RuntimeEndpointAddress(RuntimeEntityId entity, ComponentId component, EndpointId endpoint)
        implements EndpointAddress {
    /** Validates all three runtime endpoint identity dimensions. */
    RuntimeEndpointAddress {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(endpoint, "endpoint");
    }
}
