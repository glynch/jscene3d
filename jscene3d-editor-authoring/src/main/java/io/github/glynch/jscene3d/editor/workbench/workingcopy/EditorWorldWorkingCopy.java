/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.session.AuthoringChange;
import io.github.glynch.jscene3d.editor.project.session.AuthoringOperation;
import io.github.glynch.jscene3d.editor.project.session.internal.AuthoringChangeSource;
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

/** Internal authoritative working copy for one authored world definition. */
public final class EditorWorldWorkingCopy implements AutoCloseable {
    private final Path source;
    private final Deque<WorldEdit> undoHistory = new ArrayDeque<>();
    private final Deque<WorldEdit> redoHistory = new ArrayDeque<>();
    private final AuthoringChangeSource<Change> changes = new AuthoringChangeSource<>();

    private WorldDefinition current;
    private WorldDefinition saved;
    private boolean closed;

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
    }

    /**
     * Returns the normalized authored source.
     *
     * @return authored source
     */
    public Path source() {
        ensureOpen();
        return source;
    }

    /**
     * Returns whether current content differs from the saved baseline.
     *
     * @return dirty state
     */
    public boolean isDirty() {
        ensureOpen();
        return !current.equals(saved);
    }

    /**
     * Returns the internal working-copy change stream.
     *
     * @return change stream
     */
    public AuthoringChange<Change> onDidChange() {
        ensureOpen();
        return changes;
    }

    /**
     * Saves changed content atomically to the authored source.
     *
     * @throws IOException when the replacement cannot be persisted
     */
    public void save() throws IOException {
        ensureOpen();
        if (!isDirty()) {
            return;
        }
        DefinitionWriter.write(source, current);
        saved = current;
        changes.emit(Change.SAVED);
    }

    /** Reverts current content to its saved baseline and clears undo/redo history. */
    public void revert() {
        ensureOpen();
        if (!isDirty()) {
            return;
        }
        current = saved;
        undoHistory.clear();
        redoHistory.clear();
        changes.emit(Change.REVERTED);
    }

    /**
     * Returns the current in-memory world revision.
     *
     * @return current world revision
     */
    public WorldDefinition current() {
        ensureOpen();
        return current;
    }

    /**
     * Returns entity identities whose authored state differs from the saved revision.
     *
     * @return immutable modified entity identity set
     */
    public Set<EntityId> modifiedEntityIds() {
        ensureOpen();
        return WorldDefinitionEdits.modifiedEntityIds(current, saved);
    }

    /**
     * Returns the next undoable operation.
     *
     * @return next undo operation, if available
     */
    public Optional<AuthoringOperation> undoOperation() {
        ensureOpen();
        return Optional.ofNullable(undoHistory.peek()).map(WorldEdit::operation);
    }

    /**
     * Returns the next redoable operation.
     *
     * @return next redo operation, if available
     */
    public Optional<AuthoringOperation> redoOperation() {
        ensureOpen();
        return Optional.ofNullable(redoHistory.peek()).map(WorldEdit::operation);
    }

    /** Restores the immediately preceding in-memory revision. */
    public void undo() {
        ensureOpen();
        WorldEdit edit = undoHistory.poll();
        if (edit == null) {
            return;
        }
        current = edit.before();
        redoHistory.push(edit);
        changes.emit(Change.CONTENT);
    }

    /** Reapplies the immediately following in-memory revision. */
    public void redo() {
        ensureOpen();
        WorldEdit edit = redoHistory.poll();
        if (edit == null) {
            return;
        }
        current = edit.after();
        undoHistory.push(edit);
        changes.emit(Change.CONTENT);
    }

    /** Replaces one entity's enabled state and records an undo operation.
     *
     * @param entityId entity or placement identity
     * @param enabled replacement enabled state
     */
    public void setEntityEnabled(EntityId entityId, boolean enabled) {
        ensureOpen();
        WorldDefinition changed =
                WorldDefinitionEdits.setEntityEnabled(current, Objects.requireNonNull(entityId, "entityId"), enabled);
        recordEdit(AuthoringText.message("editor.operation.set-entity-enabled", "Set entity enabled"), changed);
    }

    /** Replaces one locally authored component property and records an undo operation.
     *
     * @param entityId owning entity identity
     * @param componentId component identity
     * @param propertyId property identity
     * @param value typed replacement value
     */
    public void setComponentProperty(
            EntityId entityId, ComponentId componentId, PropertyId propertyId, ProjectValue value) {
        ensureOpen();
        WorldDefinition changed = WorldDefinitionEdits.setComponentProperty(
                current,
                Objects.requireNonNull(entityId, "entityId"),
                Objects.requireNonNull(componentId, "componentId"),
                Objects.requireNonNull(propertyId, "propertyId"),
                Objects.requireNonNull(value, "value"));
        recordEdit(AuthoringText.message("editor.operation.set-component-property", "Set component property"), changed);
    }

    /** Releases listeners and rejects subsequent use. */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            changes.close();
        }
    }

    private void recordEdit(AuthoringText label, WorldDefinition changed) {
        if (changed == current) {
            return;
        }
        WorldEdit edit = new WorldEdit(new AuthoringOperation(label, source), current, changed);
        undoHistory.push(edit);
        redoHistory.clear();
        current = changed;
        changes.emit(Change.CONTENT);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("World working copy is closed");
        }
    }

    /** Internal working-copy change categories. */
    public enum Change {
        /** Current authored content changed. */
        CONTENT,
        /** Current content became the saved baseline. */
        SAVED,
        /** Current content reverted to the saved baseline. */
        REVERTED
    }

    /** One reversible world edit with its affected source metadata. */
    private record WorldEdit(AuthoringOperation operation, WorldDefinition before, WorldDefinition after) {
        private WorldEdit {
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
        }
    }
}
