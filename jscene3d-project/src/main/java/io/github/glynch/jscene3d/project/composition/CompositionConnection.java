/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.composition;

import io.github.glynch.jscene3d.project.entity.SignalConnection;
import java.util.Objects;

/** One authored connection retained in its distinct definition-instance scope.
 *
 * @param scope authored definition occurrence
 * @param connection portable authored endpoints
 * @param location diagnostic location within the root composition
 */
public record CompositionConnection(CompositionScope scope, SignalConnection connection, String location) {
    /** Validates one scoped connection. */
    public CompositionConnection {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(location, "location");
    }
}
