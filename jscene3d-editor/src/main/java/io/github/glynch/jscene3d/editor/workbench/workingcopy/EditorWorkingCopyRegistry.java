/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopies;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the live working-copy set and derives workspace-level dirty state from resource state.
 *
 * <p>The registry does not own resource content or persistence. It forwards typed lifecycle events
 * from each registered working copy.
 */
public final class EditorWorkingCopyRegistry implements EditorWorkingCopies {
    private final Map<EditorWorkingCopyId, RegisteredWorkingCopy> workingCopies = new LinkedHashMap<>();
    private final EditorEventSource<EditorWorkingCopy> registrations = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> removals = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> contentChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> dirtyChanges = new EditorEventSource<>();
    private final EditorEventSource<EditorWorkingCopy> saves = new EditorEventSource<>();

    /**
     * Registers one working copy until the returned registration is closed.
     *
     * @param workingCopy resource working copy
     * @return idempotent removal registration
     * @throws IllegalArgumentException if the same identity is already registered
     */
    public EditorRegistration register(EditorWorkingCopy workingCopy) {
        EditorWorkingCopy candidate = Objects.requireNonNull(workingCopy, "workingCopy");
        if (workingCopies.containsKey(candidate.id())) {
            throw new IllegalArgumentException("Working copy already registered: " + candidate.id());
        }
        RegisteredWorkingCopy registered = new RegisteredWorkingCopy(
                candidate,
                candidate.onDidChangeContent().subscribe(contentChanges::emit),
                candidate.onDidChangeDirty().subscribe(dirtyChanges::emit),
                candidate.onDidSave().subscribe(saves::emit));
        workingCopies.put(candidate.id(), registered);
        registrations.emit(candidate);
        AtomicBoolean active = new AtomicBoolean(true);
        return () -> {
            if (active.compareAndSet(true, false)) {
                unregister(candidate.id(), registered);
            }
        };
    }

    @Override
    public Optional<EditorWorkingCopy> find(EditorWorkingCopyId id) {
        RegisteredWorkingCopy registered = workingCopies.get(Objects.requireNonNull(id, "id"));
        return registered == null ? Optional.empty() : Optional.of(registered.workingCopy());
    }

    @Override
    public List<EditorWorkingCopy> dirtyWorkingCopies() {
        return workingCopies.values().stream()
                .map(RegisteredWorkingCopy::workingCopy)
                .filter(EditorWorkingCopy::isDirty)
                .toList();
    }

    @Override
    public boolean hasDirty() {
        return workingCopies.values().stream()
                .map(RegisteredWorkingCopy::workingCopy)
                .anyMatch(EditorWorkingCopy::isDirty);
    }

    @Override
    public int dirtyCount() {
        return (int) workingCopies.values().stream()
                .map(RegisteredWorkingCopy::workingCopy)
                .filter(EditorWorkingCopy::isDirty)
                .count();
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidRegister() {
        return registrations;
    }

    @Override
    public EditorEvent<EditorWorkingCopy> onDidUnregister() {
        return removals;
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

    private void unregister(EditorWorkingCopyId id, RegisteredWorkingCopy expected) {
        if (!workingCopies.remove(id, expected)) {
            return;
        }
        expected.close();
        removals.emit(expected.workingCopy());
    }

    /** Groups one working copy with the event subscriptions owned by its registration. */
    private record RegisteredWorkingCopy(
            EditorWorkingCopy workingCopy,
            EditorRegistration contentRegistration,
            EditorRegistration dirtyRegistration,
            EditorRegistration saveRegistration) {
        private RegisteredWorkingCopy {
            Objects.requireNonNull(workingCopy, "workingCopy");
            Objects.requireNonNull(contentRegistration, "contentRegistration");
            Objects.requireNonNull(dirtyRegistration, "dirtyRegistration");
            Objects.requireNonNull(saveRegistration, "saveRegistration");
        }

        private void close() {
            saveRegistration.close();
            dirtyRegistration.close();
            contentRegistration.close();
        }
    }
}
