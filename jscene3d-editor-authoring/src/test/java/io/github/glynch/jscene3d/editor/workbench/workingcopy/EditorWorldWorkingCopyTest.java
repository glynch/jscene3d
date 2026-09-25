/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.workbench.workingcopy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.session.AuthoringOperation;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the internal authoritative world working-copy lifecycle. */
final class EditorWorldWorkingCopyTest {
    private static final AssetId WORLD_ID = AssetId.from("3b406aba-26fb-4681-9abe-7a952c321f9a");
    private static final EntityId ENTITY_ID = EntityId.from("0b295328-b5a3-4f41-9f34-e9b4abc430a7");

    @TempDir
    private Path temporaryDirectory;

    /** Tracks dirty state, operation metadata, undo/redo, and redo clearing after a new mutation. */
    @Test
    void tracksReversibleMutations() {
        EditorWorldWorkingCopy workingCopy = workingCopy();
        List<EditorWorldWorkingCopy.Change> changes = new ArrayList<>();
        workingCopy.onDidChange().subscribe(changes::add);

        workingCopy.setEntityEnabled(ENTITY_ID, false);

        assertThat(workingCopy.isDirty()).isTrue();
        assertThat(workingCopy.current().roots().getFirst().isEnabled()).isFalse();
        assertThat(workingCopy.undoOperation())
                .contains(new AuthoringOperation(
                        AuthoringText.message("editor.operation.set-entity-enabled", "Set entity enabled"),
                        temporaryDirectory.resolve("world.json")));
        workingCopy.undo();
        assertThat(workingCopy.current().roots().getFirst().isEnabled()).isTrue();
        assertThat(workingCopy.redoOperation()).isPresent();
        workingCopy.redo();
        workingCopy.undo();
        workingCopy.setEntityEnabled(ENTITY_ID, false);
        assertThat(workingCopy.redoOperation()).isEmpty();
        assertThat(changes).containsOnly(EditorWorldWorkingCopy.Change.CONTENT);
    }

    /** Saves and reverts against the authoritative persisted baseline. */
    @Test
    void savesAndReverts() throws Exception {
        EditorWorldWorkingCopy workingCopy = workingCopy();
        List<EditorWorldWorkingCopy.Change> changes = new ArrayList<>();
        workingCopy.onDidChange().subscribe(changes::add);
        workingCopy.setEntityEnabled(ENTITY_ID, false);

        workingCopy.save();

        assertThat(workingCopy.isDirty()).isFalse();
        assertThat(temporaryDirectory.resolve("world.json")).exists();
        workingCopy.setEntityEnabled(ENTITY_ID, true);
        workingCopy.revert();
        assertThat(workingCopy.current().roots().getFirst().isEnabled()).isFalse();
        assertThat(workingCopy.undoOperation()).isEmpty();
        assertThat(changes)
                .containsExactly(
                        EditorWorldWorkingCopy.Change.CONTENT,
                        EditorWorldWorkingCopy.Change.SAVED,
                        EditorWorldWorkingCopy.Change.CONTENT,
                        EditorWorldWorkingCopy.Change.REVERTED);
    }

    /** Removes subscriptions and rejects access after disposal. */
    @Test
    void closesIdempotently() {
        EditorWorldWorkingCopy workingCopy = workingCopy();

        workingCopy.close();
        workingCopy.close();

        assertThatThrownBy(workingCopy::current)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("World working copy is closed");
    }

    private EditorWorldWorkingCopy workingCopy() {
        LocalEntity entity = new LocalEntity(ENTITY_ID, "Player", true, List.of(), List.of());
        WorldDefinition world = new WorldDefinition(WORLD_ID, "World", List.of(entity));
        return new EditorWorldWorkingCopy(temporaryDirectory.resolve("world.json"), world);
    }
}
