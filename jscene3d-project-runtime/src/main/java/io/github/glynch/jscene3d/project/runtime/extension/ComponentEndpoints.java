/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

import io.github.glynch.jscene3d.project.component.EndpointId;
import io.github.glynch.jscene3d.project.runtime.RuntimeAction;
import io.github.glynch.jscene3d.project.runtime.RuntimePayloadAction;
import io.github.glynch.jscene3d.project.runtime.RuntimeSignal;

/**
 * Short-lived descriptor-backed context for one component's signal and action implementations.
 *
 * <p>Every declared endpoint must be bound exactly once during the callback. Endpoint direction and payload presence
 * are checked against the safe descriptor. This context must not be retained; only a returned {@link RuntimeSignal}
 * may outlive the binding callback.
 */
public interface ComponentEndpoints {
    /**
     * Returns the persistent runtime handle for one declared signal.
     *
     * @param endpoint declared signal identity
     * @return signal handle owned by the world
     * @throws IllegalArgumentException if the signal is undeclared or already bound
     * @throws IllegalStateException if this context has expired
     */
    RuntimeSignal signal(EndpointId endpoint);

    /**
     * Implements one declared action which accepts no payload.
     *
     * @param endpoint declared action identity
     * @param action synchronous action callback
     * @throws IllegalArgumentException if the action is undeclared, already bound, or declares a payload
     * @throws IllegalStateException if this context has expired
     */
    void action(EndpointId endpoint, RuntimeAction action);

    /**
     * Implements one declared action which accepts its descriptor-declared payload.
     *
     * @param endpoint declared action identity
     * @param action synchronous action callback
     * @throws IllegalArgumentException if the action is undeclared, already bound, or declares no payload
     * @throws IllegalStateException if this context has expired
     */
    void action(EndpointId endpoint, RuntimePayloadAction action);
}
