/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the revisioned permanent authoring session facade through the real project loader. */
final class EditorProjectSessionTest {
    private static final EntityId ENTITY_ID = EntityId.from("0b295328-b5a3-4f41-9f34-e9b4abc430a7");
    private static final ComponentId COMPONENT_ID = ComponentId.from("3e940be7-e58d-4f3a-8b5e-e61c99c00904");
    private static final PropertyId SPEED = new PropertyId("speed");

    @TempDir
    private Path projectRoot;

    /** Applies descriptor-validated typed mutations and advances authoritative revisions and notifications. */
    @Test
    void appliesTypedMutationAndRejectsInvalidOrStaleMutation() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorHierarchyNode entity = session.hierarchy().children().getFirst();
            InspectorMutationTarget.ComponentProperty target =
                    new InspectorMutationTarget.ComponentProperty(entity.occurrence(), ENTITY_ID, COMPONENT_ID, SPEED);
            List<AuthoringSessionChange> changes = new ArrayList<>();
            List<Boolean> dirtyChanges = new ArrayList<>();
            session.onDidChange().subscribe(changes::add);
            session.onDidChangeDirty().subscribe(dirtyChanges::add);

            long revision =
                    session.mutate(target, new ProjectValue.NumberValue(new BigDecimal("2.5")), session.revision());

            assertThat(revision).isEqualTo(1L);
            assertThat(session.isDirty()).isTrue();
            assertThat(session.undoOperation())
                    .get()
                    .extracting(AuthoringOperation::label)
                    .isEqualTo(
                            AuthoringText.message("editor.operation.set-component-property", "Set component property"));
            assertThat(changes)
                    .containsExactly(new AuthoringSessionChange(1L, true, AuthoringSessionChange.Kind.CONTENT));
            assertThat(dirtyChanges).containsExactly(true);
            ProjectValue invalid = new ProjectValue.TextValue("invalid");
            assertThatThrownBy(() -> session.mutate(target, invalid, revision))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not satisfy property speed");
            ProjectValue stale = new ProjectValue.NumberValue(BigDecimal.ONE);
            assertThatThrownBy(() -> session.mutate(target, stale, 0L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("stale authoring revision");
            assertThat(session.revision()).isEqualTo(1L);
        }
    }

    /** Supports undo, redo, redo clearing, save, revert, hierarchy changes, and subscription removal. */
    @Test
    void managesWorkingCopyLifecycleThroughTheFacade() throws Exception {
        EditorProjectSession session = session();
        EditorHierarchyNode entity = session.hierarchy().children().getFirst();
        InspectorMutationTarget.EntityEnabled target =
                new InspectorMutationTarget.EntityEnabled(entity.occurrence(), ENTITY_ID);
        List<EditorHierarchyNode> hierarchyChanges = new ArrayList<>();
        AuthoringSubscription subscription = session.onDidChangeHierarchy().subscribe(hierarchyChanges::add);

        session.mutate(target, new ProjectValue.BooleanValue(false), 0L);
        session.undo();
        assertThat(session.canRedo()).isTrue();
        session.redo();
        session.undo();
        session.mutate(target, new ProjectValue.BooleanValue(false), session.revision());
        assertThat(session.canRedo()).isFalse();
        assertThat(session.revision()).isEqualTo(5L);
        session.save();
        assertThat(session.isDirty()).isFalse();
        session.mutate(target, new ProjectValue.BooleanValue(true), session.revision());
        session.revert();
        assertThat(session.startupWorld().roots().getFirst().isEnabled()).isFalse();
        assertThat(session.canUndo()).isFalse();
        assertThat(session.revision()).isEqualTo(7L);
        assertThat(hierarchyChanges).hasSize(7);

        subscription.close();
        session.mutate(target, new ProjectValue.BooleanValue(true), session.revision());
        assertThat(hierarchyChanges).hasSize(7);
        session.close();
        session.close();
        assertThatThrownBy(session::revision).isInstanceOf(IllegalStateException.class);
    }

    /** Rejects mutation targets for generated/read-only hierarchy occurrences. */
    @Test
    void rejectsReadOnlyOccurrenceMutation() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorHierarchyNode world = session.hierarchy();
            InspectorMutationTarget.EntityEnabled target =
                    new InspectorMutationTarget.EntityEnabled(world.occurrence(), ENTITY_ID);
            ProjectValue replacement = new ProjectValue.BooleanValue(false);

            assertThatThrownBy(() -> session.mutate(target, replacement, 0L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("read-only");
        }
    }

    private EditorProjectSession session() throws IOException {
        writeProject();
        return new EditorProjectLoader("0.1.0-SNAPSHOT", getClass().getClassLoader())
                .load(projectRoot)
                .session()
                .orElseThrow();
    }

    private void writeProject() throws IOException {
        write("authoring-session.j3d", """
                {
                  "$schema":"https://jscene3d.org/schemas/project-1.json",
                  "schemaVersion":1,
                  "identity":{"id":"example.session-test","name":"Session Test","version":"1.0.0"},
                  "engine":{"requires":">=0.1.0-SNAPSHOT <0.2.0"},
                  "runtime":{"applicationExtension":"example.session-test","entryScene":"worlds/main.world.json"},
                  "extensions":[{"id":"example.session-test","requires":">=1.0.0 <2.0.0"}]
                }
                """);
        write("src/main/resources/META-INF/jscene3d/extension.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/extension-1.json",
                  "schemaVersion":1,
                  "id":"example.session-test",
                  "version":"1.0.0",
                  "engineRequires":">=0.1.0-SNAPSHOT <0.2.0",
                  "displayName":"Session Test",
                  "types":[],
                  "components":[{
                    "id":"example.session-test/mover",
                    "typeVersion":1,
                    "displayName":"Mover",
                    "properties":[{
                      "id":"speed",
                      "valueKind":"number",
                      "required":true,
                      "displayName":"Speed"
                    }]
                  }]
                }
                """);
        write("worlds/main.world.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/world-definition-1.json",
                  "assetId":"3b406aba-26fb-4681-9abe-7a952c321f9a",
                  "assetType":"world-definition",
                  "formatVersion":1,
                  "name":"Opening World",
                  "connections":[],
                  "roots":[{
                    "entryType":"local",
                    "entityId":"0b295328-b5a3-4f41-9f34-e9b4abc430a7",
                    "name":"Player",
                    "enabled":true,
                    "components":[{
                      "componentId":"3e940be7-e58d-4f3a-8b5e-e61c99c00904",
                      "type":"example.session-test/mover",
                      "typeVersion":1,
                      "properties":{"speed":1}
                    }],
                    "children":[]
                  }]
                }
                """);
    }

    private void write(String relativePath, String content) throws IOException {
        Path target = projectRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
