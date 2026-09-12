/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import io.github.glynch.jscene3d.editor.command.EditorUndoRedoEntry;
import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentDefinition;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityEntry;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.EntityPlacement;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Resource working copy for one authored world definition. */
public final class EditorWorldWorkingCopy implements EditorWorkingCopy {
    /** Stable type identifier for authored-world working copies. */
    public static final String TYPE = "world";

    private final Path source;
    private final EditorWorkingCopyId id;
    private final Deque<WorldEdit> undoHistory = new ArrayDeque<>();
    private final Deque<WorldEdit> redoHistory = new ArrayDeque<>();
    private final EditorEventSource<EditorWorkingCopy> contentChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> dirtyChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> saves = new EditorEventSource<>();

    private WorldDefinition current;
    private WorldDefinition saved;

    /**
     * Creates a clean working copy from a loaded authored world.
     *
     * @param source authored world source
     * @param world loaded world revision
     */
    public EditorWorldWorkingCopy(Path source, WorldDefinition world) {
        this.source = Objects.requireNonNull(source, "source").toAbsolutePath().normalize();
        current = Objects.requireNonNull(world, "world");
        saved = current;
        id = new EditorWorkingCopyId(this.source.toUri(), TYPE);
    }

    @Override
    public EditorWorkingCopyId id() {
        return id;
    }

    @Override
    public boolean isDirty() {
        return !current.equals(saved);
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidChangeContent() {
        return contentChanges;
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidChangeDirty() {
        return dirtyChanges;
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidSave() {
        return saves;
    }

    @Override
    public void save() throws IOException {
        if (!isDirty()) {
            return;
        }
        DefinitionWriter.write(source, current);
        saved = current;
        dirtyChanges.emit(this);
        saves.emit(this);
    }

    @Override
    public void revert() {
        if (!isDirty()) {
            return;
        }
        current = saved;
        undoHistory.clear();
        redoHistory.clear();
        contentChanges.emit(this);
        dirtyChanges.emit(this);
    }

    /**
     * Returns the current in-memory world revision.
     *
     * @return current world revision
     */
    public WorldDefinition current() {
        return current;
    }

    /**
     * Returns entity identities whose authored state differs from the saved revision.
     *
     * @return immutable modified entity identity set
     */
    public Set<EntityId> modifiedEntityIds() {
        Map<EntityId, EntityEntry> savedEntries = new HashMap<>();
        indexEntries(saved.roots(), savedEntries);
        Set<EntityId> modified = new LinkedHashSet<>();
        collectModifiedEntries(current.roots(), savedEntries, modified);
        return Set.copyOf(modified);
    }

    /**
     * Returns the next undoable edit.
     *
     * @return next undo entry, if available
     */
    public Optional<EditorUndoRedoEntry> undoEntry() {
        return Optional.ofNullable(undoHistory.peek()).map(WorldEdit::description);
    }

    /**
     * Returns the next redoable edit.
     *
     * @return next redo entry, if available
     */
    public Optional<EditorUndoRedoEntry> redoEntry() {
        return Optional.ofNullable(redoHistory.peek()).map(WorldEdit::description);
    }

    /** Restores the immediately preceding in-memory revision. */
    public void undo() {
        WorldEdit edit = undoHistory.poll();
        if (edit == null) {
            return;
        }
        boolean wasDirty = isDirty();
        current = edit.before();
        redoHistory.push(edit);
        publishContentChange(wasDirty);
    }

    /** Reapplies the immediately following in-memory revision. */
    public void redo() {
        WorldEdit edit = redoHistory.poll();
        if (edit == null) {
            return;
        }
        boolean wasDirty = isDirty();
        current = edit.after();
        undoHistory.push(edit);
        publishContentChange(wasDirty);
    }

    /**
     * Replaces one entity's enabled state and records a resource-aware undo entry.
     *
     * @param entityId entity to update
     * @param enabled replacement enabled state
     */
    public void setEntityEnabled(EntityId entityId, Boolean enabled) {
        EntityId target = Objects.requireNonNull(entityId, "entityId");
        boolean replacement = Objects.requireNonNull(enabled, "enabled");
        EntryUpdate update = updateEntries(current.roots(), target, replacement);
        if (!update.changed()) {
            return;
        }
        WorldDefinition changed =
                new WorldDefinition(current.id(), current.name(), current.connections(), update.entries());
        boolean wasDirty = isDirty();
        WorldEdit edit = new WorldEdit(new EditorUndoRedoEntry("Set entity enabled", id), current, changed);
        undoHistory.push(edit);
        redoHistory.clear();
        current = changed;
        publishContentChange(wasDirty);
    }

    /**
     * Replaces one property on a locally authored component and records a resource-aware undo entry.
     *
     * @param entityId entity owning the component
     * @param componentId component to update
     * @param propertyId property to replace
     * @param value validated portable replacement value
     */
    public void setComponentProperty(
            EntityId entityId, ComponentId componentId, PropertyId propertyId, ProjectValue value) {
        EntityId targetEntity = Objects.requireNonNull(entityId, "entityId");
        ComponentId targetComponent = Objects.requireNonNull(componentId, "componentId");
        PropertyId targetProperty = Objects.requireNonNull(propertyId, "propertyId");
        ProjectValue replacement = Objects.requireNonNull(value, "value");
        EntryUpdate update = updateEntries(current.roots(), targetEntity, targetComponent, targetProperty, replacement);
        if (!update.changed()) {
            return;
        }
        WorldDefinition changed =
                new WorldDefinition(current.id(), current.name(), current.connections(), update.entries());
        boolean wasDirty = isDirty();
        WorldEdit edit = new WorldEdit(new EditorUndoRedoEntry("Set component property", id), current, changed);
        undoHistory.push(edit);
        redoHistory.clear();
        current = changed;
        publishContentChange(wasDirty);
    }

    private void publishContentChange(boolean wasDirty) {
        contentChanges.emit(this);
        if (wasDirty != isDirty()) {
            dirtyChanges.emit(this);
        }
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
        return new EntryUpdate(List.copyOf(updated), changed);
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
        return new EntryUpdate(List.copyOf(updated), changed);
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

    /** One reversible world edit with its affected resource identity. */
    private record WorldEdit(EditorUndoRedoEntry description, WorldDefinition before, WorldDefinition after) {
        private WorldEdit {
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
        }
    }

    /** Result of recursively updating one immutable entry tree. */
    private record EntryUpdate(List<EntityEntry> entries, boolean changed) {
        private EntryUpdate {
            entries = List.copyOf(entries);
        }
    }
}
