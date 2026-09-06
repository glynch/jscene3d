/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.runtime.extension;

/**
 * Binds every descriptor-declared signal and action on one runtime component.
 *
 * <p>The world invokes this callback once after component construction and authored-reference binding, but before the
 * inactive world is published. The supplied endpoint context is valid only for the duration of the callback. Signal
 * handles returned by it remain valid for the lifetime of the owning world and may emit only while that world is
 * active.
 */
@FunctionalInterface
public interface ComponentEndpointBinder {
    /**
     * Obtains signal handles and registers action callbacks for every endpoint declared by the component descriptor.
     *
     * @param endpoints short-lived descriptor-backed endpoint context
     * @throws RuntimeException when binding cannot complete; composition converts the failure to a structured
     *     diagnostic and rolls back the entire world
     */
    void bindEndpoints(ComponentEndpoints endpoints);
}
