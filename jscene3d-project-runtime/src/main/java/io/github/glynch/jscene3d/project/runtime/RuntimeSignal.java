/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime;

/**
 * Declared signal output bound to one world's synchronous connection graph.
 *
 * <p>Emission is valid only while the owning world is active. Disabled source components do not dispatch, and payload
 * identity must exactly match the descriptor declaration.
 */
public interface RuntimeSignal {
    /**
     * Emits a signal declared without a payload.
     *
     * @throws IllegalArgumentException if this signal declares a payload
     * @throws IllegalStateException if the owning world is not active
     */
    void emit();

    /**
     * Emits a signal carrying its declared payload type.
     *
     * @param payload payload matching the signal's declared type
     * @throws IllegalArgumentException if this signal declares no payload or another registered payload type
     * @throws IllegalStateException if the owning world is not active
     */
    void emit(RuntimePayload payload);
}
