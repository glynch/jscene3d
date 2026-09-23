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
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
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
        return WorldDefinitionEdits.modifiedEntityIds(current, saved);
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
        WorldDefinition changed = WorldDefinitionEdits.setEntityEnabled(current, target, replacement);
        if (changed == current) {
            return;
        }
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
        WorldDefinition changed = WorldDefinitionEdits.setComponentProperty(
                current, targetEntity, targetComponent, targetProperty, replacement);
        if (changed == current) {
            return;
        }
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

    /** One reversible world edit with its affected resource identity. */
    private record WorldEdit(EditorUndoRedoEntry description, WorldDefinition before, WorldDefinition after) {
        private WorldEdit {
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
        }
    }
}
