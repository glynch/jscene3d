/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import java.util.Objects;

/** Declarative connection from a node signal to a node action.
 *
 * @param from emitted signal
 * @param to invoked action
 */
public record SceneConnection(SignalEndpoint from, ActionEndpoint to) {
    /** Validates connection endpoints. */
    public SceneConnection {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
    }

    /** Signal emitted by one scene node.
     *
     * @param node node identifier
     * @param signal registered signal name
     */
    public record SignalEndpoint(String node, String signal) {
        /** Validates a signal endpoint. */
        public SignalEndpoint {
            requireText(node, "node");
            requireText(signal, "signal");
        }
    }

    /** Action accepted by one scene node or its controller.
     *
     * @param node node identifier
     * @param action registered action name
     */
    public record ActionEndpoint(String node, String action) {
        /** Validates an action endpoint. */
        public ActionEndpoint {
            requireText(node, "node");
            requireText(action, "action");
        }
    }

    /** Requires non-blank endpoint text. */
    private static void requireText(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
