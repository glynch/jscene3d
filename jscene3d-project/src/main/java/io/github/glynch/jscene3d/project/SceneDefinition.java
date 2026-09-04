/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Immutable, validated scene definition ready for later runtime instantiation. */
public final class SceneDefinition {
    private final Path source;
    private final String id;
    private final SceneNodeDefinition root;
    private final List<SceneConnection> connections;

    /**
     * Creates a validated scene definition.
     *
     * @param source normalized absolute source path
     * @param id stable scene identifier
     * @param root root scene node
     * @param connections signal-to-action connections
     */
    public SceneDefinition(Path source, String id, SceneNodeDefinition root, List<SceneConnection> connections) {
        this.source = requireNormalizedAbsolute(source, "source");
        this.id = requireText(id, "id");
        this.root = Objects.requireNonNull(root, "root");
        this.connections = List.copyOf(connections);
    }

    /**
     * Returns the normalized absolute source path.
     *
     * @return scene source path
     */
    public Path source() {
        return source;
    }

    /**
     * Returns the stable scene identifier.
     *
     * @return scene identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the root scene node.
     *
     * @return root node
     */
    public SceneNodeDefinition root() {
        return root;
    }

    /**
     * Returns signal-to-action connections in declaration order.
     *
     * @return immutable ordered connections
     */
    public List<SceneConnection> connections() {
        return connections;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SceneDefinition definition
                && source.equals(definition.source)
                && id.equals(definition.id)
                && root.equals(definition.root)
                && connections.equals(definition.connections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id, root, connections);
    }

    @Override
    public String toString() {
        return "SceneDefinition[source=" + source + ", id=" + id + ", root=" + root + ", connections=" + connections
                + ']';
    }

    /** Requires a non-blank string. */
    private static String requireText(String value, String name) {
        String validValue = Objects.requireNonNull(value, name);
        if (validValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validValue;
    }
}
