/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.input;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireLocalId;
import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable, structurally validated mapping from semantic actions to physical controls. */
public final class InputMapDefinition {
    private final Path source;
    private final Map<String, List<InputBinding>> actions;

    /**
     * Creates one validated input-map definition.
     *
     * @param source normalized absolute definition-document path
     * @param actions non-empty action bindings in authored order
     */
    public InputMapDefinition(Path source, Map<String, List<InputBinding>> actions) {
        this.source = requireNormalizedAbsolute(source, "source");
        this.actions = copyActions(actions);
    }

    /**
     * Returns the input-map document path.
     *
     * @return normalized absolute source path
     */
    public Path source() {
        return source;
    }

    /**
     * Returns physical bindings indexed by semantic action identifier.
     *
     * @return immutable actions in authored order
     */
    public Map<String, List<InputBinding>> actions() {
        return actions;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InputMapDefinition definition)) {
            return false;
        }
        return source.equals(definition.source) && actions.equals(definition.actions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, actions);
    }

    @Override
    public String toString() {
        return "InputMapDefinition[source=" + source + ", actions=" + actions + ']';
    }

    /** Copies actions while retaining authored order and enforcing public-model invariants. */
    private static Map<String, List<InputBinding>> copyActions(Map<String, List<InputBinding>> actions) {
        Objects.requireNonNull(actions, "actions");
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        Map<String, List<InputBinding>> copied = new LinkedHashMap<>();
        actions.forEach((action, bindings) -> {
            String validAction = requireLocalId(action, "action");
            List<InputBinding> validBindings = List.copyOf(Objects.requireNonNull(bindings, "bindings"));
            if (validBindings.isEmpty()) {
                throw new IllegalArgumentException("bindings must not be empty: " + validAction);
            }
            copied.put(validAction, validBindings);
        });
        return Collections.unmodifiableMap(copied);
    }
}
