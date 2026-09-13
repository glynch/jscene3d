/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Applies immutable entity-tree edits and compares authored world revisions. */
final class WorldDefinitionEdits {
    private WorldDefinitionEdits() {}

    /** Returns entity identities whose authored state differs between two revisions. */
    static Set<EntityId> modifiedEntityIds(WorldDefinition current, WorldDefinition saved) {
        Map<EntityId, EntityEntry> savedEntries = new HashMap<>();
        indexEntries(saved.roots(), savedEntries);
        Set<EntityId> modified = new LinkedHashSet<>();
        collectModifiedEntries(current.roots(), savedEntries, modified);
        return Set.copyOf(modified);
    }

    /** Returns a revision with one entity enabled state replaced. */
    static WorldDefinition setEntityEnabled(WorldDefinition world, EntityId target, boolean enabled) {
        EntryUpdate update = updateEntries(world.roots(), target, enabled);
        return update.changed()
                ? new WorldDefinition(world.id(), world.name(), world.connections(), update.entries())
                : world;
    }

    /** Returns a revision with one locally authored component property replaced. */
    static WorldDefinition setComponentProperty(
            WorldDefinition world,
            EntityId targetEntity,
            ComponentId targetComponent,
            PropertyId targetProperty,
            ProjectValue value) {
        EntryUpdate update = updateEntries(world.roots(), targetEntity, targetComponent, targetProperty, value);
        return update.changed()
                ? new WorldDefinition(world.id(), world.name(), world.connections(), update.entries())
                : world;
    }

    private static void indexEntries(List<EntityEntry> entries, Map<EntityId, EntityEntry> indexed) {
        for (EntityEntry entry : entries) {
            indexed.put(entry.id(), entry);
            if (entry instanceof LocalEntity local) {
                indexEntries(local.children(), indexed);
            }
        }
    }

    private static void collectModifiedEntries(
            List<EntityEntry> entries, Map<EntityId, EntityEntry> savedEntries, Set<EntityId> modified) {
        for (EntityEntry entry : entries) {
            EntityEntry savedEntry = savedEntries.get(entry.id());
            if (savedEntry == null || !sameAuthoredEntry(entry, savedEntry)) {
                modified.add(entry.id());
            }
            if (entry instanceof LocalEntity local) {
                collectModifiedEntries(local.children(), savedEntries, modified);
            }
        }
    }

    private static boolean sameAuthoredEntry(EntityEntry currentEntry, EntityEntry savedEntry) {
        return switch (currentEntry) {
            case LocalEntity local
            when savedEntry instanceof LocalEntity savedLocal ->
                local.name().equals(savedLocal.name())
                        && local.isEnabled() == savedLocal.isEnabled()
                        && local.components().equals(savedLocal.components())
                        && sameChildStructure(local.children(), savedLocal.children());
            case EntityPlacement placement
            when savedEntry instanceof EntityPlacement savedPlacement -> placement.equals(savedPlacement);
            default -> false;
        };
    }

    private static boolean sameChildStructure(List<EntityEntry> currentChildren, List<EntityEntry> savedChildren) {
        if (currentChildren.size() != savedChildren.size()) {
            return false;
        }
        for (int index = 0; index < currentChildren.size(); index++) {
            EntityEntry currentChild = currentChildren.get(index);
            EntityEntry savedChild = savedChildren.get(index);
            if (!currentChild.id().equals(savedChild.id()) || currentChild.getClass() != savedChild.getClass()) {
                return false;
            }
        }
        return true;
    }

    private static EntryUpdate updateEntries(List<EntityEntry> entries, EntityId target, boolean enabled) {
        List<EntityEntry> updated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (EntityEntry entry : entries) {
            EntityEntry replacement = entry;
            if (entry.id().equals(target)) {
                replacement = withEnabled(entry, enabled);
            } else if (entry instanceof LocalEntity local) {
                EntryUpdate children = updateEntries(local.children(), target, enabled);
                if (children.changed()) {
                    replacement = withChildren(local, children.entries());
                }
            }
            changed |= replacement != entry;
            updated.add(replacement);
        }
        return new EntryUpdate(updated, changed);
    }

    private static EntryUpdate updateEntries(
            List<EntityEntry> entries,
            EntityId targetEntity,
            ComponentId targetComponent,
            PropertyId targetProperty,
            ProjectValue value) {
        List<EntityEntry> updated = new ArrayList<>(entries.size());
        boolean changed = false;
        for (EntityEntry entry : entries) {
            EntityEntry replacement = entry;
            if (entry instanceof LocalEntity local) {
                if (local.id().equals(targetEntity)) {
                    replacement = withComponentProperty(local, targetComponent, targetProperty, value);
                } else {
                    EntryUpdate children =
                            updateEntries(local.children(), targetEntity, targetComponent, targetProperty, value);
                    if (children.changed()) {
                        replacement = withChildren(local, children.entries());
                    }
                }
            }
            changed |= replacement != entry;
            updated.add(replacement);
        }
        return new EntryUpdate(updated, changed);
    }

    private static EntityEntry withEnabled(EntityEntry entry, boolean enabled) {
        if (entry.isEnabled() == enabled) {
            return entry;
        }
        return switch (entry) {
            case LocalEntity local ->
                local.name()
                        .<EntityEntry>map(name ->
                                new LocalEntity(local.id(), name, enabled, local.components(), local.children()))
                        .orElseGet(() -> new LocalEntity(local.id(), enabled, local.components(), local.children()));
            case EntityPlacement placement ->
                placement
                        .name()
                        .<EntityEntry>map(name -> new EntityPlacement(
                                placement.id(), name, enabled, placement.definition(), placement.arguments()))
                        .orElseGet(() -> new EntityPlacement(
                                placement.id(), enabled, placement.definition(), placement.arguments()));
        };
    }

    private static LocalEntity withChildren(LocalEntity local, List<EntityEntry> children) {
        return local.name()
                .map(name -> new LocalEntity(local.id(), name, local.isEnabled(), local.components(), children))
                .orElseGet(() -> new LocalEntity(local.id(), local.isEnabled(), local.components(), children));
    }

    private static LocalEntity withComponentProperty(
            LocalEntity local, ComponentId componentId, PropertyId propertyId, ProjectValue value) {
        List<ComponentDefinition> components =
                new ArrayList<>(local.components().size());
        boolean changed = false;
        for (ComponentDefinition component : local.components()) {
            ComponentDefinition replacement = component;
            if (component.id().equals(componentId)) {
                Map<PropertyId, ProjectValue> properties = new LinkedHashMap<>(component.properties());
                ProjectValue previous = properties.put(propertyId, value);
                if (!value.equals(previous)) {
                    replacement = new ComponentDefinition(
                            component.id(), component.type(), component.typeVersion(), properties);
                }
            }
            changed |= replacement != component;
            components.add(replacement);
        }
        if (!changed) {
            return local;
        }
        return local.name()
                .map(name -> new LocalEntity(local.id(), name, local.isEnabled(), components, local.children()))
                .orElseGet(() -> new LocalEntity(local.id(), local.isEnabled(), components, local.children()));
    }

    /** Result of recursively updating one immutable entry tree. */
    private record EntryUpdate(List<EntityEntry> entries, boolean changed) {
        private EntryUpdate {
            entries = List.copyOf(entries);
        }
    }
}
