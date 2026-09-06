/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import java.util.Objects;

/** Authored connection from one stable signal target to one stable action target.
 *
 * @param signal signal source
 * @param action action destination
 */
public record SignalConnection(EndpointTarget signal, EndpointTarget action) {
    /** Validates the endpoints. */
    public SignalConnection {
        Objects.requireNonNull(signal, "signal");
        Objects.requireNonNull(action, "action");
    }
}
