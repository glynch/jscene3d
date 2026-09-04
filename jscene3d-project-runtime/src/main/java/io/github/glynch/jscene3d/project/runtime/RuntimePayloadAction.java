/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/** Action implementation for an endpoint carrying a declared payload type. */
@FunctionalInterface
public interface RuntimePayloadAction {
    /**
     * Executes the action synchronously with its typed payload.
     *
     * @param payload payload matching the endpoint's declared type
     */
    void execute(RuntimePayload payload);
}
