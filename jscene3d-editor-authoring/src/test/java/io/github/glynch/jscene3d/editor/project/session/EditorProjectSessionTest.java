/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.editor.project.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.editor.presentation.AuthoringText;
import io.github.glynch.jscene3d.editor.project.asset.ProjectAsset;
import io.github.glynch.jscene3d.editor.project.loading.EditorProjectLoader;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyNode;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjection;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.EditorHierarchyProjector;
import io.github.glynch.jscene3d.editor.workbench.hierarchy.HierarchyOccurrenceId;
import io.github.glynch.jscene3d.editor.workbench.inspector.InspectorMutationTarget;
import io.github.glynch.jscene3d.project.asset.AssetCatalog;
import io.github.glynch.jscene3d.project.asset.AssetId;
import io.github.glynch.jscene3d.project.asset.AssetKind;
import io.github.glynch.jscene3d.project.asset.DefinitionResolver;
import io.github.glynch.jscene3d.project.asset.DefinitionResolvers;
import io.github.glynch.jscene3d.project.asset.DefinitionWriter;
import io.github.glynch.jscene3d.project.component.ComponentId;
import io.github.glynch.jscene3d.project.component.PropertyId;
import io.github.glynch.jscene3d.project.diagnostic.ProjectDiagnostic;
import io.github.glynch.jscene3d.project.entity.EntityDefinition;
import io.github.glynch.jscene3d.project.entity.EntityId;
import io.github.glynch.jscene3d.project.entity.LocalEntity;
import io.github.glynch.jscene3d.project.extension.RegisteredTypeCatalog;
import io.github.glynch.jscene3d.project.manifest.GameProject;
import io.github.glynch.jscene3d.project.settings.ProjectConfiguration;
import io.github.glynch.jscene3d.project.value.ProjectValue;
import io.github.glynch.jscene3d.project.world.WorldDefinition;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies the revisioned permanent authoring session facade through the real project loader. */
final class EditorProjectSessionTest {
    private static final AssetId WORLD_ID = AssetId.from("3b406aba-26fb-4681-9abe-7a952c321f9a");
    private static final AssetId DEFINITION_ID = AssetId.from("4ccdb339-9c5b-47d3-9b18-9169be5e4936");
    private static final AssetId GENERATED_DEFINITION_ID = AssetId.from("11111111-1111-4111-8111-111111111111");
    private static final EntityId ENTITY_ID = EntityId.from("0b295328-b5a3-4f41-9f34-e9b4abc430a7");
    private static final ComponentId COMPONENT_ID = ComponentId.from("3e940be7-e58d-4f3a-8b5e-e61c99c00904");
    private static final PropertyId SPEED = new PropertyId("speed");

    @TempDir
    private Path projectRoot;

    /** Retains world and entity definitions together under their stable asset identities. */
    @Test
    void retainsMultipleStructuralDefinitions() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorRetainedDefinition world =
                    session.retainDefinition(WORLD_ID).definition().orElseThrow();
            EditorRetainedDefinition entity =
                    session.retainDefinition(DEFINITION_ID).definition().orElseThrow();

            assertThat(world)
                    .returns(AssetKind.WORLD_DEFINITION, EditorRetainedDefinition::kind)
                    .returns(EditorRetainedDefinition.Origin.AUTHORED, EditorRetainedDefinition::origin)
                    .returns(true, EditorRetainedDefinition::editable);
            assertThat(world.content()).isInstanceOf(EditorRetainedDefinition.Content.World.class);
            assertThat(world.hierarchy().roots()).hasSize(1);
            assertThat(entity)
                    .returns(AssetKind.ENTITY_DEFINITION, EditorRetainedDefinition::kind)
                    .returns(EditorRetainedDefinition.Origin.AUTHORED, EditorRetainedDefinition::origin)
                    .returns(true, EditorRetainedDefinition::editable);
            assertThat(entity.content()).isInstanceOf(EditorRetainedDefinition.Content.Entity.class);
            assertThat(entity.hierarchy().roots())
                    .singleElement()
                    .satisfies(root -> assertThat(root.occurrence().entityPath())
                            .containsExactly(root.entityId().orElseThrow()));
            assertThat(session.retainedDefinitionIds()).containsExactlyInAnyOrder(WORLD_ID, DEFINITION_ID);
        }
    }

    /** Returns resolver diagnostics without retaining an unknown structural identity. */
    @Test
    void rejectsUnknownDefinitionIdentity() throws Exception {
        try (EditorProjectSession session = session()) {
            DefinitionRetentionResult result =
                    session.retainDefinition(AssetId.from("11111111-1111-4111-8111-111111111111"));

            assertThat(result.definition()).isEmpty();
            assertThat(result.diagnostics()).isNotEmpty();
            assertThat(session.retainedDefinitionIds()).isEmpty();
        }
    }

    /** Retains currently resolvable published definitions as generated and read-only. */
    @Test
    void retainsGeneratedDefinitionAsReadOnly() throws Exception {
        URI source = URI.create("jscene3d-import:/models/generated.entity.json");
        try (EditorProjectSession session = sessionWithGeneratedDefinition(source)) {
            EditorRetainedDefinition definition = session.retainDefinition(GENERATED_DEFINITION_ID)
                    .definition()
                    .orElseThrow();

            assertThat(definition)
                    .returns(EditorRetainedDefinition.Origin.GENERATED, EditorRetainedDefinition::origin)
                    .returns(false, EditorRetainedDefinition::editable)
                    .returns(source, EditorRetainedDefinition::source);
            assertThat(definition.hierarchy().roots()).singleElement().returns(false, EditorHierarchyNode::isEditable);
        }
    }

    /** Applies descriptor-validated typed mutations and advances authoritative revisions and notifications. */
    @Test
    void appliesTypedMutationAndRejectsInvalidOrStaleMutation() throws Exception {
        try (EditorProjectSession session = session()) {
            EditorHierarchyNode entity = session.hierarchy().roots().getFirst();
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
        EditorHierarchyNode entity = session.hierarchy().roots().getFirst();
        InspectorMutationTarget.EntityEnabled target =
                new InspectorMutationTarget.EntityEnabled(entity.occurrence(), ENTITY_ID);
        List<EditorHierarchyProjection> hierarchyChanges = new ArrayList<>();
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

    /** Rejects mutation targets that are not actual projected entity occurrences. */
    @Test
    void rejectsNonEntityOccurrenceMutation() throws Exception {
        try (EditorProjectSession session = session()) {
            HierarchyOccurrenceId document =
                    new HierarchyOccurrenceId(session.startupWorld().id(), List.of());
            InspectorMutationTarget.EntityEnabled target =
                    new InspectorMutationTarget.EntityEnabled(document, ENTITY_ID);
            ProjectValue replacement = new ProjectValue.BooleanValue(false);

            assertThatThrownBy(() -> session.mutate(target, replacement, 0L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not current");
        }
    }

    private EditorProjectSession session() throws IOException {
        writeProject();
        return new EditorProjectLoader("0.1.0-SNAPSHOT", getClass().getClassLoader())
                .load(projectRoot)
                .session()
                .orElseThrow();
    }

    private EditorProjectSession sessionWithGeneratedDefinition(URI source) throws Exception {
        EditorProjectSession base = session();
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        DefinitionWriter.write(
                content,
                new EntityDefinition(
                        GENERATED_DEFINITION_ID,
                        "Generated",
                        new LocalEntity(
                                EntityId.from("22222222-2222-4222-8222-222222222222"),
                                "Generated Root",
                                true,
                                List.of(),
                                List.of())));
        DefinitionResolver definitions = DefinitionResolvers.builder(base.authoredAssets())
                .addGeneratedEntity(GENERATED_DEFINITION_ID, source, new ByteArrayInputStream(content.toByteArray()))
                .build();
        GameProject project = base.project();
        ProjectConfiguration configuration = base.configuration();
        AssetCatalog authored = base.authoredAssets();
        RegisteredTypeCatalog types = base.types();
        List<ProjectAsset> assets = base.assets();
        WorldDefinition world = base.startupWorld();
        Path worldSource = base.startupWorldSource();
        base.close();
        List<ProjectDiagnostic> diagnostics = new ArrayList<>();
        EditorHierarchyProjector projector = new EditorHierarchyProjector(definitions, types, diagnostics);
        return new EditorProjectSession(
                new EditorProjectSession.Source(
                        project, configuration, authored, types, definitions, world, worldSource),
                assets,
                projector,
                diagnostics);
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
        write("entities/reusable.entity.json", """
                {
                  "$schema":"https://jscene3d.org/schemas/entity-definition-1.json",
                  "assetId":"4ccdb339-9c5b-47d3-9b18-9169be5e4936",
                  "assetType":"entity-definition",
                  "formatVersion":1,
                  "name":"Reusable",
                  "contract":{
                    "parameters":[],
                    "signals":[],
                    "actions":[],
                    "capabilities":[],
                    "attachments":[],
                    "resourceBindings":[]
                  },
                  "connections":[],
                  "root":{
                    "entryType":"local",
                    "entityId":"7be5cdf0-1bc7-481c-8880-3d627e14d52d",
                    "name":"Reusable Root",
                    "enabled":true,
                    "components":[],
                    "children":[]
                  }
                }
                """);
    }

    private void write(String relativePath, String content) throws IOException {
        Path target = projectRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
    }
}
