/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project;

import static io.github.glynch.jscene3d.project.internal.ProjectPaths.requireNormalizedAbsolute;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable node in a loaded scene definition. */
public final class SceneNodeDefinition {
    private final String id;
    private final Optional<String> name;
    private final boolean enabled;
    private final Source source;
    private final Optional<ControllerDefinition> controller;
    private final List<SceneNodeDefinition> children;

    /**
     * Creates one validated scene node.
     *
     * @param id stable scene-wide node identifier
     * @param name optional editor display name
     * @param enabled initial enabled state
     * @param source registered node type or nested-scene instance
     * @param controller optional project controller
     * @param children immutable child definitions in scene-tree order
     */
    public SceneNodeDefinition(
            String id,
            Optional<String> name,
            boolean enabled,
            Source source,
            Optional<ControllerDefinition> controller,
            List<SceneNodeDefinition> children) {
        this.id = requireText(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.name.ifPresent(value -> requireText(value, "name"));
        this.enabled = enabled;
        this.source = Objects.requireNonNull(source, "source");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.children = List.copyOf(children);
    }

    /**
     * Returns the stable scene-wide node identifier.
     *
     * @return node identifier
     */
    public String id() {
        return id;
    }

    /**
     * Returns the optional editor display name.
     *
     * @return optional display name
     */
    public Optional<String> name() {
        return name;
    }

    /**
     * Returns whether the node starts enabled.
     *
     * @return initial enabled state
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Returns the node source.
     *
     * @return registered type or nested scene instance
     */
    public Source source() {
        return source;
    }

    /**
     * Returns the optional project controller.
     *
     * @return optional controller definition
     */
    public Optional<ControllerDefinition> controller() {
        return controller;
    }

    /**
     * Returns child definitions in scene-tree order.
     *
     * @return immutable ordered children
     */
    public List<SceneNodeDefinition> children() {
        return children;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof SceneNodeDefinition definition
                && enabled == definition.enabled
                && id.equals(definition.id)
                && name.equals(definition.name)
                && source.equals(definition.source)
                && controller.equals(definition.controller)
                && children.equals(definition.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, source, controller, children);
    }

    @Override
    public String toString() {
        return "SceneNodeDefinition[id=" + id + ", name=" + name + ", enabled=" + enabled + ", source=" + source
                + ", controller=" + controller + ", children=" + children + ']';
    }

    /** Origin of one node definition. */
    public sealed interface Source permits TypedNode, SceneInstance {}

    /** Node created from a registered node type.
     *
     * @param type registered node type
     * @param properties editable node properties
     */
    public record TypedNode(RegisteredType type, Map<String, ProjectValue> properties) implements Source {
        /** Validates and copies typed-node values. */
        public TypedNode {
            Objects.requireNonNull(type, "type");
            properties = copyValues(properties, "properties");
        }
    }

    /** Nested scene instance with local property overrides.
     *
     * @param scene normalized absolute nested-scene path
     * @param overrides editable overrides applied to the nested scene
     */
    public record SceneInstance(Path scene, Map<String, ProjectValue> overrides) implements Source {
        /** Validates and copies scene-instance values. */
        public SceneInstance {
            requireNormalizedAbsolute(scene, "scene");
            overrides = copyValues(overrides, "overrides");
        }
    }

    /** Copies project values while preserving source order. */
    private static Map<String, ProjectValue> copyValues(Map<String, ProjectValue> values, String name) {
        Objects.requireNonNull(values, name);
        Map<String, ProjectValue> copied = new LinkedHashMap<>();
        values.forEach((key, value) ->
                copied.put(Objects.requireNonNull(key, name + " key"), Objects.requireNonNull(value, name + " value")));
        return Collections.unmodifiableMap(copied);
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
