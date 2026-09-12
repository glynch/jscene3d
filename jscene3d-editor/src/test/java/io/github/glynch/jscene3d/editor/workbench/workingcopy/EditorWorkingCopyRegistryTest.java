/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.glynch.jscene3d.editor.lifecycle.EditorEvent;
import io.github.glynch.jscene3d.editor.lifecycle.EditorRegistration;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopy;
import io.github.glynch.jscene3d.editor.workingcopy.EditorWorkingCopyId;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Verifies resource-oriented working-copy registration and aggregate dirty state. */
final class EditorWorkingCopyRegistryTest {
    /** Tracks registration, content, dirty, save, and removal through the registry boundary. */
    @Test
    void derivesWorkspaceStateFromRegisteredWorkingCopies() throws IOException {
        EditorWorkingCopyRegistry registry = new EditorWorkingCopyRegistry();
        TestWorkingCopy world = new TestWorkingCopy("file:///project/worlds/map01.world.json", "world");
        TestWorkingCopy settings = new TestWorkingCopy("file:///project/.jscene3d/settings.json", "settings");
        List<EditorWorkingCopy> registered = new ArrayList<>();
        List<EditorWorkingCopy> changed = new ArrayList<>();
        List<EditorWorkingCopy> dirtyChanges = new ArrayList<>();
        List<EditorWorkingCopy> saved = new ArrayList<>();
        List<EditorWorkingCopy> unregistered = new ArrayList<>();
        registry.onDidRegister().subscribe(registered::add);
        registry.onDidChangeContent().subscribe(changed::add);
        registry.onDidChangeDirty().subscribe(dirtyChanges::add);
        registry.onDidSave().subscribe(saved::add);
        registry.onDidUnregister().subscribe(unregistered::add);

        EditorRegistration worldRegistration = registry.register(world);
        registry.register(settings);
        world.edit();
        settings.edit();

        assertThat(registered).containsExactly(world, settings);
        assertThat(changed).containsExactly(world, settings);
        assertThat(dirtyChanges).containsExactly(world, settings);
        assertThat(registry.hasDirty()).isTrue();
        assertThat(registry.dirtyCount()).isEqualTo(2);
        assertThat(registry.dirtyWorkingCopies()).containsExactly(world, settings);
        assertThat(registry.find(world.id())).contains(world);

        world.save();

        assertThat(saved).containsExactly(world);
        assertThat(registry.dirtyCount()).isEqualTo(1);
        assertThat(registry.dirtyWorkingCopies()).containsExactly(settings);

        worldRegistration.close();
        worldRegistration.close();

        assertThat(unregistered).containsExactly(world);
        assertThat(registry.find(world.id())).isEmpty();
    }

    /** Mutable boundary fake used only to drive the working-copy contract. */
    private static final class TestWorkingCopy implements EditorWorkingCopy {
        private final EditorWorkingCopyId id;
        private final TestEvent<EditorWorkingCopy> contentChanges = new TestEvent<>();
        private final TestEvent<EditorWorkingCopy> dirtyChanges = new TestEvent<>();
        private final TestEvent<EditorWorkingCopy> saves = new TestEvent<>();
        private boolean dirty;

        private TestWorkingCopy(String resource, String type) {
            id = new EditorWorkingCopyId(URI.create(resource), type);
        }

        @Override
        public EditorWorkingCopyId id() {
            return id;
        }

        @Override
        public boolean isDirty() {
            return dirty;
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
        public void save() {
            if (!dirty) {
                return;
            }
            dirty = false;
            dirtyChanges.emit(this);
            saves.emit(this);
        }

        @Override
        public void revert() {
            if (!dirty) {
                return;
            }
            dirty = false;
            contentChanges.emit(this);
            dirtyChanges.emit(this);
        }

        private void edit() {
            dirty = true;
            contentChanges.emit(this);
            dirtyChanges.emit(this);
        }
    }

    /** Minimal event driver kept at the public subscription seam. */
    private static final class TestEvent<T> implements EditorEvent<T> {
        private final List<Consumer<? super T>> listeners = new ArrayList<>();

        @Override
        public EditorRegistration subscribe(Consumer<? super T> listener) {
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        private void emit(T event) {
            List.copyOf(listeners).forEach(listener -> listener.accept(event));
        }
    }
}
