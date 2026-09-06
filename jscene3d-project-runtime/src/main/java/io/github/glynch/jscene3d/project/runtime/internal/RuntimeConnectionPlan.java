/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.internal;

import java.util.Objects;

/** One authored connection resolved to exact live component endpoint addresses. */
record RuntimeConnectionPlan(RuntimeEndpointAddress signal, RuntimeEndpointAddress action, String location) {
    /** Validates the resolved connection and its diagnostic location. */
    RuntimeConnectionPlan {
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(location, "location");
    }
}
