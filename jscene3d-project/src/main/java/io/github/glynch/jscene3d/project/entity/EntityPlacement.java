/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import static io.github.glynch.jscene3d.project.internal.Preconditions.immutableProjectValues;
import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import io.github.glynch.jscene3d.project.asset.AssetRef;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Authored placement of a reusable entity definition.
 *
 * <p>The placement ID becomes the identity of the instantiated definition root. The placement does not create an
 * additional wrapper entity.
 */
public final class EntityPlacement implements EntityEntry {
    private final EntityId id;
    private final Optional<String> name;
    private final boolean enabled;
    private final AssetRef<EntityDefinition> definition;
    private final Map<String, ProjectValue> arguments;

    /**
     * Creates an unnamed reusable-definition placement.
     *
     * @param id stable instance-root identity within the containing asset
     * @param enabled initial local enabled state
     * @param definition referenced reusable entity definition
     * @param arguments exported contract arguments
     */
    public EntityPlacement(
            EntityId id, boolean enabled, AssetRef<EntityDefinition> definition, Map<String, ProjectValue> arguments) {
        this(id, Optional.empty(), enabled, definition, arguments);
    }

    /**
     * Creates a named reusable-definition placement.
     *
     * @param id stable instance-root identity within the containing asset
     * @param name editor display name
     * @param enabled initial local enabled state
     * @param definition referenced reusable entity definition
     * @param arguments exported contract arguments
     */
    public EntityPlacement(
            EntityId id,
            String name,
            boolean enabled,
            AssetRef<EntityDefinition> definition,
            Map<String, ProjectValue> arguments) {
        this(id, Optional.of(requireNonBlank(name, "name")), enabled, definition, arguments);
    }

    /** Stores copied placement values after shared constructor validation. */
    private EntityPlacement(
            EntityId id,
            Optional<String> name,
            boolean enabled,
            AssetRef<EntityDefinition> definition,
            Map<String, ProjectValue> arguments) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.enabled = enabled;
        this.definition = Objects.requireNonNull(definition, "definition");
        this.arguments = immutableProjectValues(arguments, "arguments");
    }

    @Override
    public EntityId id() {
        return id;
    }

    @Override
    public Optional<String> name() {
        return name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the referenced reusable entity definition.
     *
     * @return typed entity-definition reference
     */
    public AssetRef<EntityDefinition> definition() {
        return definition;
    }

    /**
     * Returns immutable exported contract arguments in declaration order.
     *
     * @return placement arguments
     */
    public Map<String, ProjectValue> arguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof EntityPlacement placement
                && enabled == placement.enabled
                && id.equals(placement.id)
                && Objects.equals(name, placement.name)
                && definition.equals(placement.definition)
                && arguments.equals(placement.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, definition, arguments);
    }

    @Override
    public String toString() {
        return "EntityPlacement[id=" + id + ", name=" + name + ", enabled=" + enabled + ", definition=" + definition
                + ", arguments=" + arguments + ']';
    }
}
