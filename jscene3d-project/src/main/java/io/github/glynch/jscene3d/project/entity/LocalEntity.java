/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.project.entity;

import static io.github.glynch.jscene3d.project.internal.Preconditions.requireNonBlank;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Immutable entity authored locally within a world or reusable entity definition. */
public final class LocalEntity implements EntityEntry {
    private final EntityId id;
    private final Optional<String> name;
    private final boolean enabled;
    private final List<ComponentDefinition> components;
    private final List<EntityEntry> children;

    /**
     * Creates an unnamed local entity.
     *
     * @param id stable identity within the containing asset
     * @param enabled initial local enabled state
     * @param components authored components in declaration order
     * @param children owned child entries in declaration order
     */
    public LocalEntity(EntityId id, boolean enabled, List<ComponentDefinition> components, List<EntityEntry> children) {
        this(id, Optional.empty(), enabled, components, children);
    }

    /**
     * Creates a named local entity.
     *
     * @param id stable identity within the containing asset
     * @param name editor display name
     * @param enabled initial local enabled state
     * @param components authored components in declaration order
     * @param children owned child entries in declaration order
     */
    public LocalEntity(
            EntityId id,
            String name,
            boolean enabled,
            List<ComponentDefinition> components,
            List<EntityEntry> children) {
        this(id, Optional.of(requireNonBlank(name, "name")), enabled, components, children);
    }

    /** Stores copied entity values after shared constructor validation. */
    private LocalEntity(
            EntityId id,
            Optional<String> name,
            boolean enabled,
            List<ComponentDefinition> components,
            List<EntityEntry> children) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.enabled = enabled;
        this.components = List.copyOf(components);
        this.children = List.copyOf(children);
        requireUniqueComponentIds(this.components);
    }

    /** Rejects ambiguous duplicate component identities within this entity. */
    private static void requireUniqueComponentIds(List<ComponentDefinition> components) {
        Set<ComponentId> ids = new HashSet<>();
        for (ComponentDefinition component : components) {
            if (!ids.add(component.id())) {
                throw new IllegalArgumentException("components contains a duplicate id: " + component.id());
            }
        }
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
     * Returns authored components in declaration order.
     *
     * @return immutable components
     */
    public List<ComponentDefinition> components() {
        return components;
    }

    /**
     * Returns owned child entries in declaration order.
     *
     * @return immutable child entries
     */
    public List<EntityEntry> children() {
        return children;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof LocalEntity entity
                && enabled == entity.enabled
                && id.equals(entity.id)
                && Objects.equals(name, entity.name)
                && components.equals(entity.components)
                && children.equals(entity.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, enabled, components, children);
    }

    @Override
    public String toString() {
        return "LocalEntity[id=" + id + ", name=" + name + ", enabled=" + enabled + ", components=" + components
                + ", children=" + children + ']';
    }
}
